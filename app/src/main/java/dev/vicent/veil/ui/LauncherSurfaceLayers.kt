package dev.vicent.veil.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import dev.vicent.veil.launcher.LauncherUiState
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.ui.components.AppDrawer
import dev.vicent.veil.ui.components.LauncherPublisherInfo
import dev.vicent.veil.ui.components.LauncherSettingsScreen
import dev.vicent.veil.ui.components.LauncherSettingsUiState
import dev.vicent.veil.ui.components.SettingsAccessActions
import dev.vicent.veil.ui.components.SettingsAppActions
import dev.vicent.veil.ui.components.SettingsAppearanceActions
import dev.vicent.veil.ui.components.SettingsNavigationActions
import dev.vicent.veil.ui.theme.VeilMotion

@Composable
internal fun LauncherDrawerLayer(
    state: LauncherUiState,
    settingsShortcuts: List<SettingsShortcut>,
    navigationActions: LauncherNavigationActions,
    appActions: LauncherAppActions,
    onAppActionsRequested: (AppActionsTarget) -> Unit,
    onContinuityDisclosureRequested: () -> Unit,
) {
    AnimatedVisibility(
        visible = state.isDrawerOpen,
        enter = fadeIn(
            animationSpec = tween(
                VeilMotion.STANDARD_DURATION_MILLIS,
                easing = VeilMotion.enterEasing,
            ),
            initialAlpha = 0.72f,
        ) + slideInVertically(
            animationSpec = tween(
                VeilMotion.EMPHASIZED_DURATION_MILLIS,
                easing = VeilMotion.standardEasing,
            ),
            initialOffsetY = { it / 6 },
        ),
        exit = fadeOut(
            animationSpec = tween(
                VeilMotion.QUICK_DURATION_MILLIS,
                easing = VeilMotion.exitEasing,
            ),
        ) + slideOutVertically(
            animationSpec = tween(
                VeilMotion.STANDARD_DURATION_MILLIS,
                easing = VeilMotion.exitEasing,
            ),
            targetOffsetY = { it / 8 },
        ),
        label = "app drawer",
    ) {
        AppDrawer(
            installedApps = state.installedApps,
            searchLearning = state.searchLearning,
            settingsShortcuts = settingsShortcuts,
            isLoading = state.isLoading,
            isOpen = state.isDrawerOpen,
            onAppSelected = appActions.onSearchAppSelected,
            onAppLongPressed = { app, query ->
                onAppActionsRequested(AppActionsTarget(app = app, searchQuery = query))
            },
            onSettingsSelected = appActions.onSettingsSelected,
            onVeilSettingsSelected = navigationActions.onOpenSettings,
            continuityAccessGranted = state.continuityAccessGranted,
            onContinuityAccessSelected = onContinuityDisclosureRequested,
            onClose = navigationActions.onCloseDrawer,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
internal fun LauncherSettingsLayer(
    state: LauncherUiState,
    showFontSettings: Boolean,
    systemAccent: Color?,
    publisherInfo: LauncherPublisherInfo,
    navigationActions: LauncherNavigationActions,
    appActions: LauncherAppActions,
    accessActions: LauncherAccessActions,
    appearanceActions: LauncherAppearanceActions,
    onBack: () -> Unit,
    onFontSettingsRequested: () -> Unit,
    onDisclosureRequested: (LauncherDisclosure) -> Unit,
) {
    AnimatedVisibility(
        visible = state.isSettingsOpen,
        enter = fadeIn(
            animationSpec = tween(
                VeilMotion.STANDARD_DURATION_MILLIS,
                easing = VeilMotion.enterEasing,
            ),
        ) + slideInVertically(
            animationSpec = tween(
                VeilMotion.EMPHASIZED_DURATION_MILLIS,
                easing = VeilMotion.standardEasing,
            ),
            initialOffsetY = { it / 8 },
        ),
        exit = fadeOut(
            animationSpec = tween(
                VeilMotion.QUICK_DURATION_MILLIS,
                easing = VeilMotion.exitEasing,
            ),
        ),
        label = "launcher settings",
    ) {
        LauncherSettingsScreen(
            state = LauncherSettingsUiState(
                preferences = state.preferences,
                access = state.access,
                installedApps = state.installedApps,
                appTarget = state.settingsAppTarget,
                showFontSettings = showFontSettings,
                systemAccent = systemAccent,
                publisherInfo = publisherInfo,
            ),
            navigationActions = SettingsNavigationActions(
                onBack = onBack,
                onOpenFontSettings = onFontSettingsRequested,
            ),
            appearanceActions = SettingsAppearanceActions(
                onAccentSelected = appearanceActions.onAccentSelected,
                onHomeTextToneSelected = appearanceActions.onHomeTextToneSelected,
                onHomeTextWeightSelected = appearanceActions.onHomeTextWeightSelected,
                onWallpaperScrimEnabledChanged = appearanceActions.onWallpaperScrimEnabledChanged,
                onWallpaperScrimIntensityChanged = appearanceActions.onWallpaperScrimIntensityChanged,
                onWallpaperSelected = accessActions.onWallpaperSelected,
                onResetAppearance = appearanceActions.onResetAppearance,
            ),
            appActions = SettingsAppActions(
                onOpenMusicProviderPicker = navigationActions.onOpenMusicProviderPicker,
                onSettingsAppSelected = appActions.onSettingsAppSelected,
                onMusicProviderCleared = appActions.onMusicProviderCleared,
            ),
            accessActions = settingsAccessActions(
                state = state,
                appActions = appActions,
                accessActions = accessActions,
                onDisclosureRequested = onDisclosureRequested,
            ),
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun settingsAccessActions(
    state: LauncherUiState,
    appActions: LauncherAppActions,
    accessActions: LauncherAccessActions,
    onDisclosureRequested: (LauncherDisclosure) -> Unit,
) = SettingsAccessActions(
    onContinuitySelected = {
        if (state.access.continuityGranted) {
            accessActions.onContinuityAccessRequested()
        } else {
            onDisclosureRequested(LauncherDisclosure.CONTINUITY)
            true
        }
    },
    onCalendarSelected = {
        if (state.access.calendarGranted) {
            accessActions.onAppPermissionSettingsRequested()
        } else {
            accessActions.onCalendarPermissionRequested()
            true
        }
    },
    onLocationSelected = {
        if (state.access.approximateLocationGranted) {
            accessActions.onAppPermissionSettingsRequested()
        } else {
            onDisclosureRequested(LauncherDisclosure.LOCATION)
            true
        }
    },
    onAudioVisualizerSelected = {
        if (state.access.audioVisualizerGranted) {
            accessActions.onAppPermissionSettingsRequested()
        } else {
            onDisclosureRequested(LauncherDisclosure.AUDIO_VISUALIZER)
            true
        }
    },
    onPrivacyPolicySelected = appActions.onPrivacyPolicySelected,
    onFocusNotificationsSelected = accessActions.onFocusNotificationsSelected,
    onExactAlarmsSelected = accessActions.onExactAlarmsSelected,
    onDefaultHomeSelected = accessActions.onDefaultHomeSelected,
    onAndroidSettingsSelected = accessActions.onAndroidSettingsSelected,
)
