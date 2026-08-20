package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.launcher.model.SettingsAppTarget
import dev.vicent.veil.launcher.model.LauncherContext
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.HomeTextTone
import dev.vicent.veil.launcher.model.HomeTextWeight
import dev.vicent.veil.launcher.model.AudioChannel
import dev.vicent.veil.launcher.model.QuickNoteChecklistItem
import dev.vicent.veil.launcher.model.QuickNoteType
import dev.vicent.veil.launcher.repository.AmbientContinuityRepository
import dev.vicent.veil.launcher.repository.AudioMixerRepository
import dev.vicent.veil.launcher.repository.AppRepository
import dev.vicent.veil.launcher.repository.CalendarRepository
import dev.vicent.veil.launcher.repository.FocusTimerRepository
import dev.vicent.veil.launcher.repository.LauncherPreferencesRepository
import dev.vicent.veil.launcher.repository.QuickNotesRepository
import dev.vicent.veil.launcher.repository.SystemStatusRepository
import dev.vicent.veil.launcher.repository.SteamGameRepository
import dev.vicent.veil.launcher.repository.WeatherRepository
import dev.vicent.veil.launcher.system.LauncherAccessMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LauncherController(
    private val appRepository: AppRepository,
    private val continuityRepository: AmbientContinuityRepository,
    private val calendarRepository: CalendarRepository,
    private val weatherRepository: WeatherRepository,
    private val focusTimerRepository: FocusTimerRepository,
    private val quickNotesRepository: QuickNotesRepository,
    private val systemStatusRepository: SystemStatusRepository,
    private val audioMixerRepository: AudioMixerRepository,
    private val steamGameRepository: SteamGameRepository,
    private val preferencesRepository: LauncherPreferencesRepository,
    private val accessMonitor: LauncherAccessMonitor,
    contexts: List<LauncherContext>,
    private val quickActionCount: Int,
    private val scope: CoroutineScope,
    timeProvider: TimeProvider = SystemTimeProvider,
) {
    private val contextResolver = LauncherContextResolver(contexts, quickActionCount)

    private val mutableState = MutableStateFlow(
        LauncherUiState(
            contexts = contextResolver.emptyContexts,
            preferences = preferencesRepository.state.value,
            access = accessMonitor.snapshot(),
            gameFeed = steamGameRepository.state.value,
            isContinuityOnboardingDismissed =
                continuityRepository.isNotificationOnboardingSeen(),
        ),
    )
    val state: StateFlow<LauncherUiState> = mutableState.asStateFlow()
    private val stateSynchronizer = LauncherStateSynchronizer(
        appRepository = appRepository,
        continuityRepository = continuityRepository,
        calendarRepository = calendarRepository,
        weatherRepository = weatherRepository,
        focusTimerRepository = focusTimerRepository,
        quickNotesRepository = quickNotesRepository,
        systemStatusRepository = systemStatusRepository,
        audioMixerRepository = audioMixerRepository,
        steamGameRepository = steamGameRepository,
        preferencesRepository = preferencesRepository,
        contextResolver = contextResolver,
        state = mutableState,
        timeProvider = timeProvider,
    )
    private val navigationCoordinator = LauncherNavigationCoordinator(
        state = mutableState,
        quickActionCount = quickActionCount,
    )
    private var calendarRefreshJob: Job? = null
    private var weatherRefreshJob: Job? = null
    private var gameRefreshJob: Job? = null

    fun load() {
        stateSynchronizer.start(scope)
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

    fun setAppVisible(visible: Boolean) {
        audioMixerRepository.setAppVisible(visible)
    }

    fun setAudioVolume(channel: AudioChannel, fraction: Float) {
        audioMixerRepository.setVolume(channel, fraction)
    }

    fun setCalendarAccessGranted(granted: Boolean) {
        mutableState.update { it.copy(access = it.access.copy(calendarGranted = granted)) }
        refreshCalendar(granted)
    }

    fun setLocationAccessGranted(granted: Boolean) {
        mutableState.update {
            it.copy(access = it.access.copy(approximateLocationGranted = granted))
        }
        refreshWeather(granted)
    }

    fun refreshVisibleData() {
        systemStatusRepository.refresh()
        val currentState = mutableState.value
        val contextKind = currentState.contexts.getOrNull(currentState.activeContextIndex)
            ?.definition
            ?.kind
        refreshCalendar(currentState.calendarAccessGranted)
        if (contextKind == LauncherContextKind.CURRENT) {
            refreshWeather(currentState.locationAccessGranted)
        }
        if (contextKind == LauncherContextKind.GAME) {
            gameRefreshJob?.cancel()
            gameRefreshJob = scope.launch { steamGameRepository.refresh() }
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

    fun openDrawer() = navigationCoordinator.openDrawer()

    fun closeDrawer() = navigationCoordinator.closeDrawer()

    fun handleHomePressed() = navigationCoordinator.handleHomePressed()

    fun openSettings() = navigationCoordinator.openSettings()

    fun closeSettings() = navigationCoordinator.closeSettings()

    fun setAccentMode(mode: AccentMode) = preferencesRepository.setAccentMode(mode)

    fun setHomeTextTone(mode: HomeTextTone) = preferencesRepository.setHomeTextTone(mode)

    fun setHomeTextWeight(mode: HomeTextWeight) = preferencesRepository.setHomeTextWeight(mode)

    fun setWallpaperScrimEnabled(enabled: Boolean) =
        preferencesRepository.setWallpaperScrimEnabled(enabled)

    fun setWallpaperScrimIntensity(intensity: Float) =
        preferencesRepository.setWallpaperScrimIntensity(intensity)

    fun resetAppearance() = preferencesRepository.resetAppearance()

    fun openMusicProviderPicker() = navigationCoordinator.openMusicProviderPicker()

    fun openContextSlotPicker(kind: LauncherContextKind, slotIndex: Int) {
        navigationCoordinator.openContextSlotPicker(kind, slotIndex)
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
        navigationCoordinator.completeAppSelection()
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

    fun refreshAccessState() {
        val access = accessMonitor.snapshot()
        setContinuityAccessGranted(access.continuityGranted)
        setAudioVisualizerPermissionGranted(access.audioVisualizerGranted)
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
        stateSynchronizer.removeUnavailableApp(packageName)
    }

    fun refreshApps() {
        stateSynchronizer.refreshApps(scope)
    }

    private fun LauncherUiState.contextPackageSlots(kind: LauncherContextKind): List<String?> =
        contexts.firstOrNull { it.definition.kind == kind }
            ?.quickActions
            ?.map { action -> (action as? ResolvedQuickAction.App)?.app?.packageName }
            .orEmpty()

    private fun refreshCalendar(accessGranted: Boolean) {
        calendarRefreshJob?.cancel()
        calendarRefreshJob = scope.launch { calendarRepository.refresh(accessGranted) }
    }

    private fun refreshWeather(accessGranted: Boolean) {
        weatherRefreshJob?.cancel()
        weatherRefreshJob = scope.launch { weatherRepository.refresh(accessGranted) }
    }
}
