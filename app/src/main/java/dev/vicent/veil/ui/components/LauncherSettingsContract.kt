package dev.vicent.veil.ui.components

import androidx.compose.ui.graphics.Color
import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.launcher.model.HomeTextTone
import dev.vicent.veil.launcher.model.HomeTextWeight
import dev.vicent.veil.launcher.model.LauncherAccessState
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.SettingsAppTarget

data class LauncherSettingsUiState(
    val preferences: LauncherPreferences,
    val access: LauncherAccessState,
    val installedApps: List<LauncherApp>,
    val appTarget: SettingsAppTarget?,
    val showFontSettings: Boolean,
    val systemAccent: Color?,
)

data class SettingsNavigationActions(
    val onBack: () -> Unit,
    val onOpenFontSettings: () -> Unit,
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
    val onSettingsAppSelected: (String) -> Unit,
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
