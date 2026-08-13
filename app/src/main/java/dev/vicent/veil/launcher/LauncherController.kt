package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherContext
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.CalendarEventSummary
import dev.vicent.veil.launcher.model.AudioChannel
import dev.vicent.veil.launcher.model.AudioMixerState
import dev.vicent.veil.launcher.model.FocusTimerState
import dev.vicent.veil.launcher.model.QuickActionSpec
import dev.vicent.veil.launcher.model.QuickNote
import dev.vicent.veil.launcher.model.QuickNoteChecklistItem
import dev.vicent.veil.launcher.model.QuickNoteType
import dev.vicent.veil.launcher.model.SystemStatus
import dev.vicent.veil.launcher.model.WeatherState
import dev.vicent.veil.launcher.repository.AmbientContinuityRepository
import dev.vicent.veil.launcher.repository.AudioMixerRepository
import dev.vicent.veil.launcher.repository.AppRepository
import dev.vicent.veil.launcher.repository.CalendarRepository
import dev.vicent.veil.launcher.repository.FocusTimerRepository
import dev.vicent.veil.launcher.repository.QuickNotesRepository
import dev.vicent.veil.launcher.repository.SystemStatusRepository
import dev.vicent.veil.launcher.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResolvedLauncherContext(
    val definition: LauncherContext,
    val apps: List<LauncherApp>,
    val quickActions: List<ResolvedQuickAction> = emptyList(),
)

sealed interface ResolvedQuickAction {
    data class App(val app: LauncherApp) : ResolvedQuickAction
    data class Setting(val id: String) : ResolvedQuickAction
}

data class LauncherUiState(
    val contexts: List<ResolvedLauncherContext>,
    val installedApps: List<LauncherApp> = emptyList(),
    val activeContextIndex: Int = 0,
    val isLoading: Boolean = true,
    val isDrawerOpen: Boolean = false,
    val continuityAccessGranted: Boolean = false,
    val notificationIndicatorPackages: Set<String> = emptySet(),
    val currentContinuity: ContinuityItem? = null,
    val mediaContinuity: ContinuityItem.Media? = null,
    val workProgress: ContinuityItem.Progress? = null,
    val calendarEvents: List<CalendarEventSummary> = emptyList(),
    val calendarAccessGranted: Boolean = false,
    val weather: WeatherState = WeatherState(),
    val locationAccessGranted: Boolean = false,
    val focusTimer: FocusTimerState = FocusTimerState(),
    val quickNotes: List<QuickNote> = emptyList(),
    val systemStatus: SystemStatus = SystemStatus(),
    val audioMixer: AudioMixerState = AudioMixerState(),
    val isContinuityOnboardingDismissed: Boolean = false,
)

class LauncherController(
    private val appRepository: AppRepository,
    private val continuityRepository: AmbientContinuityRepository,
    private val calendarRepository: CalendarRepository,
    private val weatherRepository: WeatherRepository,
    private val focusTimerRepository: FocusTimerRepository,
    private val quickNotesRepository: QuickNotesRepository,
    private val systemStatusRepository: SystemStatusRepository,
    private val audioMixerRepository: AudioMixerRepository,
    contexts: List<LauncherContext>,
    private val quickActionCount: Int,
) {
    private val initialContexts = contexts.map { context ->
        ResolvedLauncherContext(definition = context, apps = emptyList())
    }

    private val mutableState = MutableStateFlow(
        LauncherUiState(
            contexts = initialContexts,
            isContinuityOnboardingDismissed =
                continuityRepository.isNotificationOnboardingSeen(),
        ),
    )
    val state: StateFlow<LauncherUiState> = mutableState.asStateFlow()

    private var hasLoaded = false

    fun load(scope: CoroutineScope) {
        if (hasLoaded) return
        hasLoaded = true

        continuityRepository.start(scope)
        audioMixerRepository.start(scope)
        calendarRepository.startObserving(scope) { mutableState.value.calendarAccessGranted }
        focusTimerRepository.startObserving(scope)
        scope.launch {
            continuityRepository.items.collect { items ->
                val now = System.currentTimeMillis()
                mutableState.update { currentState ->
                    val selectedMedia = ContinuityRanker.selectMedia(items, now)
                    audioMixerRepository.setMediaPlaying(selectedMedia?.isPlaying == true)
                    currentState.copy(
                        currentContinuity = ContinuityRanker.selectCurrent(items, now),
                        mediaContinuity = selectedMedia,
                        workProgress = ContinuityRanker.selectWorkProgress(
                            items = items,
                            workPackages = currentState.contexts
                                .firstOrNull { it.definition.kind == LauncherContextKind.WORK }
                                ?.apps
                                ?.mapTo(mutableSetOf(), LauncherApp::packageName)
                                .orEmpty(),
                            nowMillis = now,
                        ),
                    )
                }
            }
        }
        scope.launch {
            continuityRepository.notificationIndicatorPackages.collect { packages ->
                mutableState.update { it.copy(notificationIndicatorPackages = packages) }
            }
        }

        scope.launch {
            val installedApps = appRepository.loadLaunchableApps()
            mutableState.update { currentState ->
                currentState.withInstalledApps(installedApps).copy(isLoading = false)
            }
        }

        scope.launch {
            calendarRepository.events.collect { events ->
                mutableState.update { it.copy(calendarEvents = events) }
            }
        }
        scope.launch {
            weatherRepository.state.collect { weather ->
                mutableState.update { it.copy(weather = weather) }
            }
        }
        scope.launch {
            focusTimerRepository.state.collect { focus ->
                mutableState.update { it.copy(focusTimer = focus) }
            }
        }
        scope.launch {
            quickNotesRepository.notes.collect { notes ->
                mutableState.update { it.copy(quickNotes = notes) }
            }
        }
        scope.launch {
            systemStatusRepository.status.collect { systemStatus ->
                mutableState.update { it.copy(systemStatus = systemStatus) }
            }
        }
        scope.launch {
            audioMixerRepository.state.collect { audioMixer ->
                mutableState.update { it.copy(audioMixer = audioMixer) }
            }
        }
        systemStatusRepository.refresh()
    }

    fun setContinuityAccessGranted(granted: Boolean) {
        continuityRepository.setAccessEnabled(granted)
        if (granted) continuityRepository.markNotificationOnboardingSeen()
        mutableState.update {
            it.copy(
                continuityAccessGranted = granted,
                notificationIndicatorPackages = if (granted) {
                    it.notificationIndicatorPackages
                } else {
                    emptySet()
                },
                isContinuityOnboardingDismissed =
                    it.isContinuityOnboardingDismissed || granted,
            )
        }
    }

    fun dismissContinuityOnboarding() {
        continuityRepository.markNotificationOnboardingSeen()
        mutableState.update { it.copy(isContinuityOnboardingDismissed = true) }
    }

    fun performContinuityAction(itemId: String, action: ContinuityAction, positionMillis: Long? = null) {
        continuityRepository.perform(itemId, action, positionMillis)
    }

    fun dismissHomeMedia(itemId: String) {
        continuityRepository.pauseMedia(itemId)
    }

    fun setAudioVisualizerPermissionGranted(granted: Boolean) {
        audioMixerRepository.setVisualizerPermissionGranted(granted)
    }

    fun setAudioVolume(channel: AudioChannel, fraction: Float) {
        audioMixerRepository.setVolume(channel, fraction)
    }

    fun setCalendarAccessGranted(granted: Boolean, scope: CoroutineScope) {
        mutableState.update { it.copy(calendarAccessGranted = granted) }
        scope.launch { calendarRepository.refresh(granted) }
    }

    fun setLocationAccessGranted(granted: Boolean, scope: CoroutineScope) {
        mutableState.update { it.copy(locationAccessGranted = granted) }
        scope.launch { weatherRepository.refresh(granted) }
    }

    fun refreshVisibleData(scope: CoroutineScope) {
        systemStatusRepository.refresh()
        scope.launch { calendarRepository.refresh(mutableState.value.calendarAccessGranted) }
        if (mutableState.value.contexts.getOrNull(mutableState.value.activeContextIndex)
                ?.definition?.kind == LauncherContextKind.CURRENT
        ) {
            scope.launch { weatherRepository.refresh(mutableState.value.locationAccessGranted) }
        }
    }

    fun openCalendarEvent(eventId: Long) = calendarRepository.open(eventId)
    fun createCalendarEvent() = calendarRepository.createEvent()
    fun openCalendar() = calendarRepository.openCalendar()
    fun configureGoogleCalendar() = calendarRepository.configureGoogleCalendar()

    fun startFocus(minutes: Int) = focusTimerRepository.start(minutes)
    fun pauseFocus() = focusTimerRepository.pause()
    fun resumeFocus() = focusTimerRepository.resume()
    fun finishFocus() = focusTimerRepository.finish()
    fun restoreFocusAlarm() = focusTimerRepository.restoreScheduledAlarm()
    fun addQuickNote(
        title: String,
        type: QuickNoteType,
        body: String,
        checklist: List<QuickNoteChecklistItem>,
    ) = quickNotesRepository.add(title, type, body, checklist)
    fun updateQuickNote(
        id: Long,
        title: String,
        type: QuickNoteType,
        body: String,
        checklist: List<QuickNoteChecklistItem>,
    ) = quickNotesRepository.update(id, title, type, body, checklist)
    fun deleteQuickNote(id: Long) = quickNotesRepository.delete(id)

    fun openDrawer() {
        mutableState.update { currentState -> currentState.copy(isDrawerOpen = true) }
    }

    fun closeDrawer() {
        mutableState.update { currentState -> currentState.copy(isDrawerOpen = false) }
    }

    fun toggleDrawer() {
        mutableState.update { currentState ->
            currentState.copy(isDrawerOpen = !currentState.isDrawerOpen)
        }
    }

    fun handleHomePressed() {
        mutableState.update { currentState ->
            when {
                currentState.isDrawerOpen -> currentState.copy(isDrawerOpen = false)
                else -> currentState.copy(isDrawerOpen = true)
            }
        }
    }

    fun selectContext(index: Int) {
        if (index !in mutableState.value.contexts.indices) return
        audioMixerRepository.setMediaWorkspaceVisible(
            mutableState.value.contexts[index].definition.kind == LauncherContextKind.MEDIA,
        )
        mutableState.update { currentState ->
            currentState.copy(activeContextIndex = index)
        }
    }

    fun removeUnavailableApp(packageName: String) {
        mutableState.update { currentState ->
            val availableApps = currentState.installedApps.filterNot {
                it.packageName == packageName
            }
            currentState.withInstalledApps(availableApps)
        }
    }

    fun refreshApps(scope: CoroutineScope) {
        scope.launch {
            val installedApps = appRepository.refreshLaunchableApps()
            mutableState.update { currentState ->
                currentState.withInstalledApps(installedApps)
            }
        }
    }

    private fun LauncherUiState.withInstalledApps(
        installedApps: List<LauncherApp>,
    ): LauncherUiState {
        val resolvedContexts = resolveContexts(installedApps)
        val workPackages = resolvedContexts
            .firstOrNull { it.definition.kind == LauncherContextKind.WORK }
            ?.apps
            ?.mapTo(mutableSetOf(), LauncherApp::packageName)
            .orEmpty()
        return copy(
            installedApps = installedApps,
            contexts = resolvedContexts,
            workProgress = ContinuityRanker.selectWorkProgress(
                continuityRepository.items.value,
                workPackages,
                System.currentTimeMillis(),
            ),
        )
    }

    private fun resolveContexts(installedApps: List<LauncherApp>): List<ResolvedLauncherContext> {
        val appsByPackage = installedApps.associateBy(LauncherApp::packageName)
        val candidates = installedApps.map { AppCandidate(it.packageName, it.category) }

        return initialContexts.map { context ->
            val configuredPackages = context.definition.quickActions
                .filterIsInstance<QuickActionSpec.App>()
                .map(QuickActionSpec.App::packageName)
            val apps = ContextAppSelector.selectQuickSlots(
                kind = context.definition.kind,
                configuredPackageNames = configuredPackages,
                installedApps = candidates,
                count = quickActionCount,
            ).mapNotNull(appsByPackage::get)
            val appIterator = apps.iterator()
            val quickActions = context.definition.quickActions.mapNotNull { spec ->
                when (spec) {
                    is QuickActionSpec.App -> if (appIterator.hasNext()) {
                        ResolvedQuickAction.App(appIterator.next())
                    } else null
                    is QuickActionSpec.Setting -> ResolvedQuickAction.Setting(spec.id)
                }
            }.take(quickActionCount)

            context.copy(apps = apps, quickActions = quickActions)
        }
    }
}
