package dev.vicent.veil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.vicent.veil.launcher.LauncherUiState
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.ui.components.AppCluster
import dev.vicent.veil.ui.components.AppDrawer
import dev.vicent.veil.ui.components.AppActionsBottomSheet
import dev.vicent.veil.ui.components.TopBar
import kotlin.math.abs

@Composable
fun LauncherScreen(
    state: LauncherUiState,
    settingsShortcuts: List<SettingsShortcut>,
    onContextSelected: (Int) -> Unit,
    onContextStep: (Int) -> Unit,
    onOpenDrawer: () -> Unit,
    onCloseDrawer: () -> Unit,
    onAppSelected: (LauncherApp) -> Unit,
    onSettingsSelected: (SettingsShortcut) -> Unit,
    onAppInfoSelected: (LauncherApp) -> Unit,
    onAppUninstallSelected: (LauncherApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeContext = state.contexts.getOrNull(state.activeContextIndex)
    var appWithOpenActions by remember { mutableStateOf<LauncherApp?>(null) }

    LaunchedEffect(state.isDrawerOpen) {
        appWithOpenActions = null
    }
    val homeGestureModifier = if (state.isDrawerOpen) {
        Modifier
    } else {
        Modifier.pointerInput(state.contexts.size, state.activeContextIndex) {
            var horizontalDistance = 0f
            var verticalDistance = 0f
            val threshold = 72.dp.toPx()

            detectDragGestures(
                onDragStart = {
                    horizontalDistance = 0f
                    verticalDistance = 0f
                },
                onDrag = { change, dragAmount ->
                    horizontalDistance += dragAmount.x
                    verticalDistance += dragAmount.y
                    if (
                        abs(horizontalDistance) >= threshold ||
                        abs(verticalDistance) >= threshold
                    ) {
                        change.consume()
                    }
                },
                onDragEnd = {
                    when {
                        verticalDistance <= -threshold &&
                            abs(verticalDistance) > abs(horizontalDistance) -> {
                            onOpenDrawer()
                        }
                        horizontalDistance <= -threshold &&
                            abs(horizontalDistance) > abs(verticalDistance) -> {
                            onContextStep(1)
                        }
                        horizontalDistance >= threshold &&
                            abs(horizontalDistance) > abs(verticalDistance) -> {
                            onContextStep(-1)
                        }
                    }
                    horizontalDistance = 0f
                    verticalDistance = 0f
                },
                onDragCancel = {
                    horizontalDistance = 0f
                    verticalDistance = 0f
                },
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(homeGestureModifier),
    ) {
        TopBar(
            contexts = state.contexts.map { it.definition },
            activeContextIndex = state.activeContextIndex,
            onContextSelected = onContextSelected,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        if (activeContext != null && !state.isLoading) {
            val horizontalPadding = if (maxWidth < 360.dp) 24.dp else 32.dp
            val bottomPadding = (maxHeight * 0.07f).coerceIn(40.dp, 88.dp)

            AppCluster(
                context = activeContext,
                onAppSelected = onAppSelected,
                onAppLongPressed = { appWithOpenActions = it },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(start = horizontalPadding, bottom = bottomPadding),
            )
        }

        if (state.isDrawerOpen) {
            AppDrawer(
                installedApps = state.installedApps,
                settingsShortcuts = settingsShortcuts,
                isLoading = state.isLoading,
                onAppSelected = onAppSelected,
                onAppLongPressed = { appWithOpenActions = it },
                onSettingsSelected = onSettingsSelected,
                onClose = onCloseDrawer,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    BackHandler(enabled = state.isDrawerOpen, onBack = onCloseDrawer)

    appWithOpenActions?.let { app ->
        AppActionsBottomSheet(
            app = app,
            onDismiss = { appWithOpenActions = null },
            onOpen = {
                appWithOpenActions = null
                onAppSelected(app)
            },
            onAppInfo = {
                appWithOpenActions = null
                onAppInfoSelected(app)
            },
            onUninstall = {
                appWithOpenActions = null
                onAppUninstallSelected(app)
            },
        )
    }
}
