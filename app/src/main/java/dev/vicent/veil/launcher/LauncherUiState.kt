package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.AudioMixerState
import dev.vicent.veil.launcher.model.AppSearchLearningState
import dev.vicent.veil.launcher.model.CalendarEventSummary
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.model.FocusTimerState
import dev.vicent.veil.launcher.model.GameFeedState
import dev.vicent.veil.launcher.model.LauncherAccessState
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherContext
import dev.vicent.veil.launcher.model.LauncherNavigationState
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.LauncherSurface
import dev.vicent.veil.launcher.model.QuickNote
import dev.vicent.veil.launcher.model.SettingsAppTarget
import dev.vicent.veil.launcher.model.SystemStatus
import dev.vicent.veil.launcher.model.WeatherState

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
    val searchLearning: AppSearchLearningState = AppSearchLearningState(),
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
    val gameFeed: GameFeedState = GameFeedState(),
    val isContinuityOnboardingDismissed: Boolean = false,
) {
    val isDrawerOpen: Boolean get() = navigation.surface == LauncherSurface.EVERYTHING
    val isSettingsOpen: Boolean get() = navigation.surface == LauncherSurface.SETTINGS
    val continuityAccessGranted: Boolean get() = access.continuityGranted
    val calendarAccessGranted: Boolean get() = access.calendarGranted
    val locationAccessGranted: Boolean get() = access.approximateLocationGranted
}
