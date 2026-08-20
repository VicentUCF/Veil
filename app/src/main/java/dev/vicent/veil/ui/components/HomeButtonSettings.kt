package dev.vicent.veil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.HomeButtonActionSpec
import dev.vicent.veil.launcher.model.HomeButtonGesture
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
internal fun HomeButtonSettings(
    preferences: LauncherPreferences,
    installedApps: List<LauncherApp>,
    settingsShortcuts: List<SettingsShortcut>,
    onBack: () -> Unit,
    onOpenPicker: (HomeButtonGesture) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.drawerBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        SettingsHeader(
            title = stringResource(R.string.home_button_settings_header),
            onBack = onBack,
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "description") {
                SettingsDescription(stringResource(R.string.home_button_settings_description))
            }
            item(key = "tap") {
                SettingsActionRow(
                    title = stringResource(R.string.settings_home_button_tap),
                    detail = preferences.homeButtonConfig.onTap.label(
                        installedApps,
                        settingsShortcuts,
                    ),
                    status = stringResource(R.string.state_change),
                    onClick = { onOpenPicker(HomeButtonGesture.TAP) },
                )
            }
            item(key = "long-press") {
                SettingsActionRow(
                    title = stringResource(R.string.settings_home_button_long_press),
                    detail = preferences.homeButtonConfig.onLongPress.label(
                        installedApps,
                        settingsShortcuts,
                    ),
                    status = stringResource(R.string.state_change),
                    onClick = { onOpenPicker(HomeButtonGesture.LONG_PRESS) },
                )
            }
            item(key = "bottom-space") { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun HomeButtonActionSpec.label(
    installedApps: List<LauncherApp>,
    settingsShortcuts: List<SettingsShortcut>,
): String = when (this) {
    HomeButtonActionSpec.Everything -> stringResource(R.string.home_button_action_everything)
    HomeButtonActionSpec.VeilSettings -> stringResource(R.string.home_button_action_veil_settings)
    is HomeButtonActionSpec.App -> packageCandidates
        .firstNotNullOfOrNull { packageName ->
            installedApps.firstOrNull { it.packageName == packageName }?.label
        }
        ?: packageCandidates.firstOrNull()
        ?: stringResource(R.string.home_button_action_app_unavailable)
    is HomeButtonActionSpec.Setting -> settingsShortcuts.firstOrNull { it.id == id }?.label
        ?: stringResource(R.string.home_button_action_setting_unavailable)
}
