package dev.vicent.veil.ui

import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.launcher.model.AudioChannel
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.HomeTextTone
import dev.vicent.veil.launcher.model.HomeTextWeight
import dev.vicent.veil.launcher.model.HomeButtonActionSpec
import dev.vicent.veil.launcher.model.HomeButtonGesture
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.QuickNoteChecklistItem
import dev.vicent.veil.launcher.model.QuickNoteType
import dev.vicent.veil.launcher.model.SettingsShortcut

data class LauncherNavigationActions(
    val onContextSelected: (Int) -> Unit,
    val onOpenDrawer: () -> Unit,
    val onCloseDrawer: () -> Unit,
    val onOpenSettings: () -> Unit,
    val onCloseSettings: () -> Unit,
    val onOpenMusicProviderPicker: () -> Unit,
    val onOpenHomeButtonPicker: (HomeButtonGesture) -> Unit,
    val onOpenContextSlotPicker: (LauncherContextKind, Int) -> Unit,
    val onHomeButtonTap: () -> Unit,
    val onHomeButtonLongPress: () -> Unit,
)

data class LauncherAppActions(
    val onAppSelected: (LauncherApp) -> Unit,
    val onSearchAppSelected: (LauncherApp, String) -> Unit,
    val onSettingsSelected: (SettingsShortcut) -> Unit,
    val onAppInfoSelected: (LauncherApp) -> Unit,
    val onAppUninstallSelected: (LauncherApp) -> Unit,
    val onExternalLinkSelected: (String) -> Unit,
    val onPrivacyPolicySelected: () -> Boolean,
    val onSettingsAppSelected: (String) -> Unit,
    val onHomeButtonActionSelected: (HomeButtonActionSpec) -> Unit,
    val onMusicProviderCleared: () -> Unit,
    val onContextSlotCleared: (LauncherContextKind, Int) -> Unit,
)

data class LauncherAccessActions(
    val onContinuityAccessRequested: () -> Boolean,
    val onContinuityOnboardingDismissed: () -> Unit,
    val onCalendarPermissionRequested: () -> Unit,
    val onLocationPermissionRequested: () -> Unit,
    val onAudioVisualizerPermissionRequested: () -> Unit,
    val onWallpaperSelected: () -> Boolean,
    val onAppPermissionSettingsRequested: () -> Boolean,
    val onFocusNotificationsSelected: () -> Boolean,
    val onExactAlarmsSelected: () -> Boolean,
    val onDefaultHomeSelected: () -> Boolean,
    val onAndroidSettingsSelected: () -> Boolean,
)

data class LauncherWorkspaceActions(
    val onClockOpenRequested: () -> Unit,
    val onCalendarEventSelected: (Long) -> Unit,
    val onCalendarEventCreateRequested: () -> Unit,
    val onCalendarOpenRequested: () -> Unit,
    val onGoogleCalendarConfigureRequested: () -> Unit,
    val onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
    val onHomeMediaDismissed: (String) -> Unit,
    val onAudioVolumeChanged: (AudioChannel, Float) -> Unit,
    val onFocusStartRequested: (Int) -> Unit,
    val onFocusPause: () -> Unit,
    val onFocusResume: () -> Unit,
    val onFocusFinish: () -> Unit,
    val onQuickNoteAdded: (String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    val onQuickNoteUpdated: (Long, String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    val onQuickNoteDeleted: (Long) -> Unit,
)

data class LauncherAppearanceActions(
    val onAccentSelected: (AccentMode) -> Unit,
    val onHomeTextToneSelected: (HomeTextTone) -> Unit,
    val onHomeTextWeightSelected: (HomeTextWeight) -> Unit,
    val onWallpaperScrimEnabledChanged: (Boolean) -> Unit,
    val onWallpaperScrimIntensityChanged: (Float) -> Unit,
    val onResetAppearance: () -> Unit,
)

data class LauncherCatalogActions(
    val onWorkspaceReplaced: (Int, LauncherContextKind) -> Unit,
    val onWorkspaceMoved: (Int, Int) -> Unit,
    val onWorkspaceSetupCompleted: () -> Unit,
)
