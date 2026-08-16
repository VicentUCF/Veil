package dev.vicent.veil.ui.components

import dev.vicent.veil.launcher.LauncherUiState
import dev.vicent.veil.launcher.model.AudioMixerState
import dev.vicent.veil.launcher.model.CalendarEventSummary
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.model.FocusTimerState
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.QuickNote
import dev.vicent.veil.launcher.model.SystemStatus
import dev.vicent.veil.launcher.model.WeatherState

internal data class CurrentHomeUiState(
    val preferences: LauncherPreferences,
    val weather: WeatherState,
    val mediaContinuity: ContinuityItem.Media?,
    val notificationIndicatorPackages: Set<String>,
)

internal data class WorkWorkspaceUiState(
    val calendarAccessGranted: Boolean,
    val calendarEvents: List<CalendarEventSummary>,
    val workProgress: ContinuityItem.Progress?,
    val quickNotes: List<QuickNote>,
    val focusTimer: FocusTimerState,
)

internal data class MediaWorkspaceUiState(
    val mediaContinuity: ContinuityItem.Media?,
    val continuityAccessGranted: Boolean,
    val audioMixer: AudioMixerState,
    val musicProvider: LauncherApp?,
)

internal fun LauncherUiState.currentHomeState() = CurrentHomeUiState(
    preferences = preferences,
    weather = weather,
    mediaContinuity = mediaContinuity,
    notificationIndicatorPackages = notificationIndicatorPackages,
)

internal fun LauncherUiState.workWorkspaceState() = WorkWorkspaceUiState(
    calendarAccessGranted = calendarAccessGranted,
    calendarEvents = calendarEvents,
    workProgress = workProgress,
    quickNotes = quickNotes,
    focusTimer = focusTimer,
)

internal fun LauncherUiState.mediaWorkspaceState() = MediaWorkspaceUiState(
    mediaContinuity = mediaContinuity,
    continuityAccessGranted = continuityAccessGranted,
    audioMixer = audioMixer,
    musicProvider = preferences.musicProviderPackage?.let { packageName ->
        installedApps.firstOrNull { app -> app.packageName == packageName }
    },
)

internal fun LauncherUiState.toolsWorkspaceState(): SystemStatus = systemStatus
