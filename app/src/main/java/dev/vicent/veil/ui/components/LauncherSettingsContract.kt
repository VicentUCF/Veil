package dev.vicent.veil.ui.components

import androidx.compose.ui.graphics.Color
import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.launcher.model.HomeTextTone
import dev.vicent.veil.launcher.model.HomeTextWeight
import dev.vicent.veil.launcher.model.LauncherAccessState
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.SettingsAppTarget
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.launcher.model.HomeButtonActionSpec
import dev.vicent.veil.launcher.model.HomeButtonGesture

data class LauncherSettingsUiState(
    val preferences: LauncherPreferences,
    val access: LauncherAccessState,
    val installedApps: List<LauncherApp>,
    val settingsShortcuts: List<SettingsShortcut>,
    val appTarget: SettingsAppTarget?,
    val showFontSettings: Boolean,
    val showHomeButtonSettings: Boolean,
    val systemAccent: Color?,
    val publisherInfo: LauncherPublisherInfo,
)

data class LauncherPublisherInfo(
    val privacyPolicyUrl: String,
    val privacyContact: String,
)

data class SettingsNavigationActions(
    val onBack: () -> Unit,
    val onOpenFontSettings: () -> Unit,
    val onOpenHomeButtonSettings: () -> Unit,
)

data class SettingsAppearanceActions(
    val onAccentSelected: (AccentMode) -> Unit,
    val onHomeTextToneSelected: (HomeTextTone) -> Unit,
    val onHomeTextWeightSelected: (HomeTextWeight) -> Unit,
    val onWallpaperScrimEnabledChanged: (Boolean) -> Unit,
    val onWallpaperScrimIntensityChanged: (Float) -> Unit,
    val onWallpaperSelected: () -> Boolean,
    val onResetAppearance: () -> Unit,
)

data class SettingsAppActions(
    val onOpenMusicProviderPicker: () -> Unit,
    val onOpenHomeButtonPicker: (HomeButtonGesture) -> Unit,
    val onSettingsAppSelected: (String) -> Unit,
    val onHomeButtonActionSelected: (HomeButtonActionSpec) -> Unit,
    val onMusicProviderCleared: () -> Unit,
)

data class SettingsAccessActions(
    val onContinuitySelected: () -> Boolean,
    val onCalendarSelected: () -> Boolean,
    val onLocationSelected: () -> Boolean,
    val onAudioVisualizerSelected: () -> Boolean,
    val onPrivacyPolicySelected: () -> Boolean,
    val onFocusNotificationsSelected: () -> Boolean,
    val onExactAlarmsSelected: () -> Boolean,
    val onDefaultHomeSelected: () -> Boolean,
    val onAndroidSettingsSelected: () -> Boolean,
)
