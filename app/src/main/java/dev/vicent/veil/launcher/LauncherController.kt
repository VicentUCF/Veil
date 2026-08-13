package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.launcher.model.LauncherAccessState
import dev.vicent.veil.launcher.model.LauncherNavigationState
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.LauncherSurface
import dev.vicent.veil.launcher.model.SettingsAppTarget
import dev.vicent.veil.launcher.model.LauncherContext
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.HomeTextTone
import dev.vicent.veil.launcher.model.HomeTextWeight
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
import dev.vicent.veil.launcher.repository.LauncherPreferencesRepository
import dev.vicent.veil.launcher.repository.QuickNotesRepository
import dev.vicent.veil.launcher.repository.SystemStatusRepository
import dev.vicent.veil.launcher.repository.WeatherRepository
import dev.vicent.veil.launcher.system.LauncherAccessMonitor
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
    data class Empty(val slotIndex: Int) : ResolvedQuickAction
}

data class LauncherUiState(
    val contexts: List<ResolvedLauncherContext>,
    val installedApps: List<LauncherApp> = emptyList(),
    val activeContextIndex: Int = 0,
    val isLoading: Boolean = true,
    val navigation: LauncherNavigationState = LauncherNavigationState(),
    val preferences: LauncherPreferences = LauncherPreferences(),
    val access: LauncherAccessState = LauncherAccessState(),
    val settingsAppTarget: SettingsAppTarget? = null,
    val settingsPickerReturnsToSettings: Boolean = false,
    val notificationIndicatorPackages: Set<String> = emptySet(),
    val currentContinuity: ContinuityItem? = null,
    val mediaContinuity: ContinuityItem.Media? = null,
    val workProgress: ContinuityItem.Progress? = null,
    val calendarEvents: List<CalendarEventSummary> = emptyList(),
    val weather: WeatherState = WeatherState(),
    val focusTimer: FocusTimerState = FocusTimerState(),
    val quickNotes: List<QuickNote> = emptyList(),
    val systemStatus: SystemStatus = SystemStatus(),
    val audioMixer: AudioMixerState = AudioMixerState(),
    val isContinuityOnboardingDismissed: Boolean = false,
) {
    val isDrawerOpen: Boolean get() = navigation.surface == LauncherSurface.EVERYTHING
    val isSettingsOpen: Boolean get() = navigation.surface == LauncherSurface.SETTINGS
    val continuityAccessGranted: Boolean get() = access.continuityGranted
    val calendarAccessGranted: Boolean get() = access.calendarGranted
    val locationAccessGranted: Boolean get() = access.approximateLocationGranted
}

class LauncherController(
    private val appRepository: AppRepository,
    private val continuityRepository: AmbientContinuityRepository,
    private val calendarRepository: CalendarRepository,
    private val weatherRepository: WeatherRepository,
    private val focusTimerRepository: FocusTimerRepository,
    private val quickNotesRepository: QuickNotesRepository,
    private val systemStatusRepository: SystemStatusRepository,
    private val audioMixerRepository: AudioMixerRepository,
    private val preferencesRepository: LauncherPreferencesRepository,
    private val accessMonitor: LauncherAccessMonitor,
    contexts: List<LauncherContext>,
    private val quickActionCount: Int,
) {
    private val initialContexts = contexts.map { context ->
        ResolvedLauncherContext(definition = context, apps = emptyList())
    }

    private val mutableState = MutableStateFlow(
        LauncherUiState(
            contexts = initialContexts,
            preferences = preferencesRepository.state.value,
            access = accessMonitor.snapshot(),
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
        systemStatusRepository.start(scope)
        calendarRepository.startObserving(scope) { mutableState.value.calendarAccessGranted }
        focusTimerRepository.startObserving(scope)
        scope.launch {
            preferencesRepository.state.collect { preferences ->
                mutableState.update { currentState ->
                    currentState.copy(preferences = preferences)
                        .withInstalledApps(currentState.installedApps)
                }
            }
        }
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
    }

    fun setContinuityAccessGranted(granted: Boolean) {
        continuityRepository.setAccessEnabled(granted)
        if (granted) continuityRepository.markNotificationOnboardingSeen()
        mutableState.update {
            it.copy(
                access = it.access.copy(continuityGranted = granted),
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
        mutableState.update {
            it.copy(access = it.access.copy(audioVisualizerGranted = granted))
        }
    }

    fun setAudioVolume(channel: AudioChannel, fraction: Float) {
        audioMixerRepository.setVolume(channel, fraction)
    }

    fun setCalendarAccessGranted(granted: Boolean, scope: CoroutineScope) {
        mutableState.update { it.copy(access = it.access.copy(calendarGranted = granted)) }
        scope.launch { calendarRepository.refresh(granted) }
    }

    fun setLocationAccessGranted(granted: Boolean, scope: CoroutineScope) {
        mutableState.update {
            it.copy(access = it.access.copy(approximateLocationGranted = granted))
        }
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
        mutableState.update { currentState ->
            currentState.copy(navigation = currentState.navigation.openEverything())
        }
    }

    fun closeDrawer() {
        mutableState.update { currentState ->
            currentState.copy(navigation = currentState.navigation.closeToHome())
        }
    }

    fun toggleDrawer() {
        mutableState.update { currentState ->
            currentState.copy(
                navigation = if (currentState.isDrawerOpen) {
                    currentState.navigation.closeToHome()
                } else {
                    currentState.navigation.openEverything()
                },
            )
        }
    }

    fun handleHomePressed() {
        mutableState.update { currentState ->
            currentState.copy(navigation = currentState.navigation.handleHomePressed())
        }
    }

    fun openSettings() {
        mutableState.update { currentState ->
            currentState.copy(
                navigation = if (currentState.isSettingsOpen) {
                    currentState.navigation
                } else {
                    currentState.navigation.openSettings()
                },
                settingsAppTarget = null,
                settingsPickerReturnsToSettings = false,
            )
        }
    }

    fun closeSettings() {
        mutableState.update { currentState ->
            if (currentState.settingsAppTarget != null) {
                if (currentState.settingsPickerReturnsToSettings) {
                    currentState.copy(
                        settingsAppTarget = null,
                        settingsPickerReturnsToSettings = false,
                    )
                } else {
                    currentState.copy(
                        navigation = currentState.navigation.closeSettings(),
                        settingsAppTarget = null,
                        settingsPickerReturnsToSettings = false,
                    )
                }
            } else {
                currentState.copy(navigation = currentState.navigation.closeSettings())
            }
        }
    }

    fun setAccentMode(mode: AccentMode) = preferencesRepository.setAccentMode(mode)

    fun setHomeTextTone(mode: HomeTextTone) = preferencesRepository.setHomeTextTone(mode)

    fun setHomeTextWeight(mode: HomeTextWeight) = preferencesRepository.setHomeTextWeight(mode)

    fun setWallpaperScrimEnabled(enabled: Boolean) =
        preferencesRepository.setWallpaperScrimEnabled(enabled)

    fun setWallpaperScrimIntensity(intensity: Float) =
        preferencesRepository.setWallpaperScrimIntensity(intensity)

    fun resetAppearance() = preferencesRepository.resetAppearance()

    fun openMusicProviderPicker() {
        mutableState.update { currentState ->
            currentState.copy(
                navigation = if (currentState.isSettingsOpen) {
                    currentState.navigation
                } else {
                    currentState.navigation.openSettings()
                },
                settingsAppTarget = SettingsAppTarget.MusicProvider,
                settingsPickerReturnsToSettings = currentState.isSettingsOpen,
            )
        }
    }

    fun openContextSlotPicker(kind: LauncherContextKind, slotIndex: Int) {
        if (slotIndex !in 0 until quickActionCount) return
        mutableState.update { currentState ->
            currentState.copy(
                navigation = currentState.navigation.openSettings(),
                settingsAppTarget = SettingsAppTarget.ContextSlot(kind, slotIndex),
                settingsPickerReturnsToSettings = false,
            )
        }
    }

    fun selectSettingsApp(packageName: String) {
        val currentState = mutableState.value
        when (val target = currentState.settingsAppTarget) {
            SettingsAppTarget.MusicProvider -> preferencesRepository.setMusicProvider(packageName)
            is SettingsAppTarget.ContextSlot -> preferencesRepository.setContextSlot(
                kind = target.kind,
                slotIndex = target.slotIndex,
                packageName = packageName,
                currentSlots = currentState.contextPackageSlots(target.kind),
            )
            null -> return
        }
        mutableState.update {
            if (it.settingsPickerReturnsToSettings) {
                it.copy(
                    settingsAppTarget = null,
                    settingsPickerReturnsToSettings = false,
                )
            } else {
                it.copy(
                    navigation = it.navigation.closeSettings(),
                    settingsAppTarget = null,
                    settingsPickerReturnsToSettings = false,
                )
            }
        }
    }

    fun clearMusicProvider() = preferencesRepository.setMusicProvider(null)

    fun clearContextSlot(kind: LauncherContextKind, slotIndex: Int) {
        preferencesRepository.setContextSlot(
            kind = kind,
            slotIndex = slotIndex,
            packageName = null,
            currentSlots = mutableState.value.contextPackageSlots(kind),
        )
    }

    fun resetContextApps(kind: LauncherContextKind) = preferencesRepository.resetContext(kind)

    fun refreshAccessState(scope: CoroutineScope) {
        val access = accessMonitor.snapshot()
        setContinuityAccessGranted(access.continuityGranted)
        setAudioVisualizerPermissionGranted(access.audioVisualizerGranted)
        setCalendarAccessGranted(access.calendarGranted, scope)
        setLocationAccessGranted(access.approximateLocationGranted, scope)
        mutableState.update { it.copy(access = access) }
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
        val resolvedContexts = resolveContexts(
            installedApps = installedApps,
            preferences = preferences,
        )
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

    private fun resolveContexts(
        installedApps: List<LauncherApp>,
        preferences: LauncherPreferences,
    ): List<ResolvedLauncherContext> {
        val appsByPackage = installedApps.associateBy(LauncherApp::packageName)
        val candidates = installedApps.map { AppCandidate(it.packageName, it.category) }

        return initialContexts.map { context ->
            val override = preferences.contextAppOverrides[context.definition.kind]
            if (override != null) {
                val slots = (override + List(quickActionCount) { null }).take(quickActionCount)
                val quickActions = slots.mapIndexed { index, packageName ->
                    packageName?.let(appsByPackage::get)
                        ?.let(ResolvedQuickAction::App)
                        ?: ResolvedQuickAction.Empty(index)
                }
                return@map context.copy(
                    apps = quickActions.mapNotNull { (it as? ResolvedQuickAction.App)?.app },
                    quickActions = quickActions,
                )
            }
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
            val resolvedDefaults = context.definition.quickActions.mapNotNull { spec ->
                when (spec) {
                    is QuickActionSpec.App -> if (appIterator.hasNext()) {
                        ResolvedQuickAction.App(appIterator.next())
                    } else null
                    is QuickActionSpec.Setting -> ResolvedQuickAction.Setting(spec.id)
                }
            }.take(quickActionCount)
            val quickActions = (
                resolvedDefaults + List(quickActionCount) { index ->
                    ResolvedQuickAction.Empty(resolvedDefaults.size + index)
                }
            ).take(quickActionCount)

            context.copy(apps = apps, quickActions = quickActions)
        }
    }

    private fun LauncherUiState.contextPackageSlots(kind: LauncherContextKind): List<String?> =
        contexts.firstOrNull { it.definition.kind == kind }
            ?.quickActions
            ?.map { action -> (action as? ResolvedQuickAction.App)?.app?.packageName }
            .orEmpty()
}
