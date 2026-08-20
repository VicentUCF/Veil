package dev.vicent.veil.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import dev.vicent.veil.R
import dev.vicent.veil.config.AccentPalette
import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.launcher.model.LauncherAccessState
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.HomeTextTone
import dev.vicent.veil.launcher.model.HomeTextWeight
import dev.vicent.veil.launcher.model.SettingsAppTarget
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
fun LauncherSettingsScreen(
    state: LauncherSettingsUiState,
    navigationActions: SettingsNavigationActions,
    appearanceActions: SettingsAppearanceActions,
    appActions: SettingsAppActions,
    accessActions: SettingsAccessActions,
    modifier: Modifier = Modifier,
) {
    val (
        preferences,
        access,
        installedApps,
        settingsShortcuts,
        appTarget,
        showFontSettings,
        showHomeButtonSettings,
        systemAccent,
        publisherInfo,
    ) = state
    val (onBack, onOpenFontSettings, onOpenHomeButtonSettings) = navigationActions
    val (
        onAccentSelected,
        onHomeTextToneSelected,
        onHomeTextWeightSelected,
        onWallpaperScrimEnabledChanged,
        onWallpaperScrimIntensityChanged,
        onWallpaperSelected,
        onResetAppearance,
    ) = appearanceActions
    val (
        onOpenMusicProviderPicker,
        onOpenHomeButtonPicker,
        onSettingsAppSelected,
        onHomeButtonActionSelected,
        onMusicProviderCleared,
    ) = appActions
    val (
        onContinuitySelected,
        onCalendarSelected,
        onLocationSelected,
        onAudioVisualizerSelected,
        onPrivacyPolicySelected,
        onFocusNotificationsSelected,
        onExactAlarmsSelected,
        onDefaultHomeSelected,
        onAndroidSettingsSelected,
    ) = accessActions
    val palette = LocalVeilPalette.current
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showExternalError by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    fun launch(action: () -> Boolean) {
        if (!action()) showExternalError = true
    }

    if (appTarget != null) {
        SettingsAppPicker(
            target = appTarget,
            installedApps = installedApps,
            settingsShortcuts = settingsShortcuts,
            onBack = onBack,
            onSelected = onSettingsAppSelected,
            onHomeButtonActionSelected = onHomeButtonActionSelected,
            modifier = modifier,
        )
        return
    }

    if (showFontSettings) {
        CurrentHomeAppearanceSettings(
            preferences = preferences,
            onBack = onBack,
            onHomeTextToneSelected = onHomeTextToneSelected,
            onHomeTextWeightSelected = onHomeTextWeightSelected,
            onWallpaperScrimEnabledChanged = onWallpaperScrimEnabledChanged,
            onWallpaperScrimIntensityChanged = onWallpaperScrimIntensityChanged,
            modifier = modifier,
        )
        return
    }

    if (showHomeButtonSettings) {
        HomeButtonSettings(
            preferences = preferences,
            installedApps = installedApps,
            settingsShortcuts = settingsShortcuts,
            onBack = onBack,
            onOpenPicker = onOpenHomeButtonPicker,
            modifier = modifier,
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.drawerBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        SettingsHeader(onBack = onBack)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "appearance-label") {
                SettingsSectionLabel(stringResource(R.string.settings_section_appearance))
            }
            item(key = "accent-intro") {
                SettingsDescription(stringResource(R.string.settings_accent_description))
            }
            items(
                count = AccentPalette.presets.size,
                key = { "accent-${AccentPalette.presets[it].mode.persistedValue}" },
            ) { index ->
                val preset = AccentPalette.presets[index]
                AccentChoiceRow(
                    label = stringResource(preset.mode.labelResource()),
                    color = preset.color,
                    selected = preferences.accentMode == preset.mode,
                    enabled = true,
                    detail = null,
                    onClick = { onAccentSelected(preset.mode) },
                )
            }
            item(key = "accent-system") {
                val available = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && systemAccent != null
                AccentChoiceRow(
                    label = stringResource(R.string.settings_system_accent),
                    color = systemAccent ?: palette.contentMuted,
                    selected = preferences.accentMode == AccentMode.SYSTEM,
                    enabled = available,
                    detail = if (available) {
                        stringResource(R.string.settings_dynamic_color)
                    } else {
                        stringResource(R.string.settings_dynamic_color_unavailable)
                    },
                    onClick = { onAccentSelected(AccentMode.SYSTEM) },
                )
            }
            item(key = "wallpaper") {
                SettingsActionRow(
                    title = stringResource(R.string.settings_wallpaper_title),
                    detail = stringResource(R.string.settings_wallpaper_detail),
                    status = stringResource(R.string.state_change),
                    onClick = { launch(onWallpaperSelected) },
                )
            }
            item(key = "home-font") {
                SettingsActionRow(
                    title = stringResource(R.string.settings_current_font_title),
                    detail = stringResource(
                        R.string.settings_current_font_detail,
                        preferences.homeTextTone.pluralLabel(),
                        preferences.homeTextWeight.label(),
                    ),
                    status = stringResource(R.string.state_open),
                    onClick = onOpenFontSettings,
                )
            }

            item(key = "apps-label") {
                SettingsSectionLabel(stringResource(R.string.settings_section_apps))
            }
            item(key = "home-button-settings") {
                SettingsActionRow(
                    title = stringResource(R.string.settings_home_button_title),
                    detail = stringResource(R.string.settings_home_button_description),
                    status = stringResource(R.string.state_open),
                    onClick = onOpenHomeButtonSettings,
                )
            }
            item(key = "music-provider") {
                val provider = preferences.musicProviderPackage?.let { packageName ->
                    installedApps.firstOrNull { it.packageName == packageName }
                }
                ConfiguredAppRow(
                    slotLabel = stringResource(R.string.settings_music_provider),
                    app = provider,
                    emptyDetail = stringResource(R.string.settings_music_provider_empty),
                    onClick = onOpenMusicProviderPicker,
                    onClear = if (preferences.musicProviderPackage != null) {
                        onMusicProviderCleared
                    } else null,
                )
            }
            item(key = "context-edit-hint") {
                SettingsDescription(
                    stringResource(R.string.settings_context_edit_hint),
                )
            }

            item(key = "access-label") {
                SettingsSectionLabel(stringResource(R.string.settings_section_access_privacy))
            }
            item(key = "continuity") {
                SettingsActionRow(
                    title = stringResource(R.string.settings_continuity_title),
                    detail = stringResource(R.string.settings_continuity_detail),
                    status = access.continuityGranted.statusLabel(),
                    onClick = { launch(onContinuitySelected) },
                )
            }
            item(key = "calendar") {
                SettingsActionRow(
                    title = stringResource(R.string.settings_calendar_title),
                    detail = stringResource(R.string.settings_calendar_detail),
                    status = access.calendarGranted.statusLabel(),
                    onClick = { launch(onCalendarSelected) },
                )
            }
            item(key = "location") {
                SettingsActionRow(
                    title = stringResource(R.string.settings_location_title),
                    detail = stringResource(R.string.settings_location_detail),
                    status = access.approximateLocationGranted.statusLabel(),
                    onClick = { launch(onLocationSelected) },
                )
            }
            item(key = "audio") {
                SettingsActionRow(
                    title = stringResource(R.string.settings_audio_title),
                    detail = stringResource(R.string.settings_audio_detail),
                    status = access.audioVisualizerGranted.statusLabel(),
                    onClick = { launch(onAudioVisualizerSelected) },
                )
            }
            item(key = "privacy-policy") {
                SettingsActionRow(
                    title = stringResource(R.string.settings_privacy_title),
                    detail = stringResource(R.string.settings_privacy_detail),
                    status = stringResource(R.string.state_read),
                    onClick = { showPrivacyPolicy = true },
                )
            }
            item(key = "focus-notifications") {
                SettingsActionRow(
                    title = stringResource(R.string.settings_focus_notifications_title),
                    detail = stringResource(R.string.settings_focus_notifications_detail),
                    status = access.focusNotificationsGranted.statusLabel(),
                    onClick = { launch(onFocusNotificationsSelected) },
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item(key = "exact-alarms") {
                    SettingsActionRow(
                        title = stringResource(R.string.settings_exact_alarms_title),
                        detail = if (access.exactAlarmsGranted) {
                            stringResource(R.string.settings_exact_alarms_allowed)
                        } else {
                            stringResource(R.string.settings_exact_alarms_fallback)
                        },
                        status = if (access.exactAlarmsGranted) {
                            stringResource(R.string.settings_exact)
                        } else {
                            stringResource(R.string.settings_approximate)
                        },
                        onClick = { launch(onExactAlarmsSelected) },
                    )
                }
            }

            item(key = "system-label") {
                SettingsSectionLabel(stringResource(R.string.settings_section_system))
            }
            item(key = "default-home") {
                SettingsActionRow(
                    title = stringResource(R.string.settings_default_home_title),
                    detail = if (access.isDefaultHome) {
                        stringResource(R.string.settings_default_home_active)
                    } else {
                        stringResource(R.string.settings_default_home_inactive)
                    },
                    status = if (access.isDefaultHome) {
                        stringResource(R.string.state_active)
                    } else {
                        stringResource(R.string.state_choose)
                    },
                    onClick = { launch(onDefaultHomeSelected) },
                )
            }
            item(key = "android-settings") {
                SettingsActionRow(
                    title = stringResource(R.string.settings_android_title),
                    detail = stringResource(R.string.settings_android_detail),
                    status = stringResource(R.string.state_open),
                    onClick = { launch(onAndroidSettingsSelected) },
                )
            }

            item(key = "reset-label") {
                SettingsSectionLabel(stringResource(R.string.settings_section_reset))
            }
            item(key = "reset") {
                SettingsActionRow(
                    title = stringResource(R.string.settings_reset_title),
                    detail = stringResource(R.string.settings_reset_detail),
                    status = stringResource(R.string.state_restore),
                    danger = true,
                    onClick = { showResetConfirmation = true },
                )
            }
            item(key = "bottom-space") { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }

    if (showResetConfirmation) {
        RofiDialog(
            title = stringResource(R.string.settings_reset_dialog_title),
            onDismiss = { showResetConfirmation = false },
            actions = {
                RofiAction(stringResource(R.string.action_cancel), { showResetConfirmation = false })
                RofiAction(
                    label = stringResource(R.string.action_restore),
                    danger = true,
                    onClick = {
                        showResetConfirmation = false
                        onResetAppearance()
                    },
                )
            },
        ) {
            RofiBody(stringResource(R.string.settings_reset_dialog_body))
        }
    }

    if (showPrivacyPolicy) {
        RofiDialog(
            title = stringResource(R.string.settings_privacy_dialog_title),
            onDismiss = { showPrivacyPolicy = false },
            actions = {
                if (publisherInfo.privacyPolicyUrl.isNotBlank()) {
                    RofiAction(
                        stringResource(R.string.settings_privacy_open_web),
                        { launch(onPrivacyPolicySelected) },
                    )
                }
                RofiAction(stringResource(R.string.action_close), { showPrivacyPolicy = false })
            },
        ) {
            val privacyBody = buildString {
                append(stringResource(R.string.settings_privacy_body))
                if (publisherInfo.privacyContact.isNotBlank()) {
                    append("\n\n")
                    append(
                        stringResource(
                            R.string.settings_privacy_contact,
                            publisherInfo.privacyContact,
                        ),
                    )
                }
            }
            RofiBody(privacyBody)
            if (publisherInfo.privacyPolicyUrl.isNotBlank()) {
                RofiBody(publisherInfo.privacyPolicyUrl)
            }
        }
    }

    if (showExternalError) {
        RofiDialog(
            title = stringResource(R.string.settings_external_error_title),
            onDismiss = { showExternalError = false },
            actions = {
                RofiAction(stringResource(R.string.action_close), { showExternalError = false })
            },
        ) {
            RofiBody(stringResource(R.string.settings_external_error_body))
        }
    }
}

private fun AccentMode.labelResource(): Int = when (this) {
    AccentMode.VEIL -> R.string.accent_veil
    AccentMode.AMBER -> R.string.accent_amber
    AccentMode.SAGE -> R.string.accent_sage
    AccentMode.SKY -> R.string.accent_sky
    AccentMode.LILAC -> R.string.accent_lilac
    AccentMode.SYSTEM -> R.string.settings_system_accent
}

@Composable
private fun Boolean.statusLabel(): String = if (this) {
    stringResource(R.string.state_review)
} else {
    stringResource(R.string.state_activate)
}
