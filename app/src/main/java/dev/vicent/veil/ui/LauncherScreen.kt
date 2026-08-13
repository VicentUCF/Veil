package dev.vicent.veil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.vicent.veil.launcher.LauncherUiState
import dev.vicent.veil.launcher.WorkspaceDataPolicy
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.ui.components.AppActionsBottomSheet
import dev.vicent.veil.ui.components.AppDrawer
import dev.vicent.veil.ui.components.ContextDock
import dev.vicent.veil.ui.components.TopBar
import dev.vicent.veil.ui.components.WorkspaceDashboard
import dev.vicent.veil.ui.theme.VeilMotion
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
    onContinuityAccessRequested: () -> Unit,
    onCalendarPermissionRequested: () -> Unit,
    onLocationPermissionRequested: () -> Unit,
    onCalendarEventSelected: (Long) -> Unit,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
    onHomeMediaDismissed: (String) -> Unit,
    onFocusStartRequested: (Int) -> Unit,
    onFocusPause: () -> Unit,
    onFocusResume: () -> Unit,
    onFocusFinish: () -> Unit,
    onHomeButtonTap: () -> Unit,
    onHomeButtonLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeContext = state.contexts.getOrNull(state.activeContextIndex)
    var appWithOpenActions by remember { mutableStateOf<LauncherApp?>(null) }
    var pendingFocusMinutes by remember { mutableIntStateOf(0) }
    var showLocationDisclosure by remember { mutableStateOf(false) }
    val showsContextDock = activeContext?.definition?.kind
        ?.let(WorkspaceDataPolicy::showsContextDock) == true
    var lastDockContextIndex by remember { mutableIntStateOf(state.activeContextIndex) }
    val displayedDockContextIndex = if (showsContextDock) {
        state.activeContextIndex
    } else {
        lastDockContextIndex
    }

    SideEffect {
        if (showsContextDock) lastDockContextIndex = state.activeContextIndex
    }

    LaunchedEffect(state.isDrawerOpen) {
        if (state.isDrawerOpen) appWithOpenActions = null
    }
    val gesturesEnabled = !state.isDrawerOpen
    val homeGestureModifier = if (gesturesEnabled) {
        Modifier.pointerInput(state.contexts.size, state.activeContextIndex) {
            var horizontalDistance = 0f
            var verticalDistance = 0f
            val threshold = 72.dp.toPx()
            detectDragGestures(
                onDragStart = { horizontalDistance = 0f; verticalDistance = 0f },
                onDrag = { change, dragAmount ->
                    if (change.isConsumed) return@detectDragGestures
                    horizontalDistance += dragAmount.x
                    verticalDistance += dragAmount.y
                    if (abs(horizontalDistance) >= threshold || abs(verticalDistance) >= threshold) {
                        change.consume()
                    }
                },
                onDragEnd = {
                    when {
                        verticalDistance <= -threshold && abs(verticalDistance) > abs(horizontalDistance) -> onOpenDrawer()
                        horizontalDistance <= -threshold && abs(horizontalDistance) > abs(verticalDistance) -> onContextStep(1)
                        horizontalDistance >= threshold && abs(horizontalDistance) > abs(verticalDistance) -> onContextStep(-1)
                    }
                },
                onDragCancel = { horizontalDistance = 0f; verticalDistance = 0f },
            )
        }
    } else Modifier

    Box(modifier = modifier.fillMaxSize().then(homeGestureModifier)) {
        if (activeContext != null) {
            AnimatedContent(
                targetState = state.activeContextIndex,
                transitionSpec = {
                    val direction = if (targetState >= initialState) 1 else -1
                    (
                        fadeIn(
                            tween(
                                VeilMotion.StandardDurationMillis,
                                easing = VeilMotion.enterEasing,
                            ),
                        ) + slideInHorizontally(
                            tween(
                                VeilMotion.StandardDurationMillis,
                                easing = VeilMotion.standardEasing,
                            ),
                        ) { it * direction / 14 }
                    ).togetherWith(
                        fadeOut(
                            tween(
                                VeilMotion.QuickDurationMillis,
                                easing = VeilMotion.exitEasing,
                            ),
                        ) + slideOutHorizontally(
                            tween(
                                VeilMotion.StandardDurationMillis,
                                easing = VeilMotion.standardEasing,
                            ),
                        ) { -it * direction / 18 },
                    )
                },
                label = "workspace",
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            ) { index ->
                state.contexts.getOrNull(index)?.let { renderedContext ->
                    WorkspaceDashboard(
                        state = state,
                        context = renderedContext,
                        settingsShortcuts = settingsShortcuts,
                        onCalendarPermissionRequested = onCalendarPermissionRequested,
                        onLocationPermissionRequested = { showLocationDisclosure = true },
                        onContinuityAccessRequested = onContinuityAccessRequested,
                        onCalendarEventSelected = onCalendarEventSelected,
                        onContinuityAction = onContinuityAction,
                        onHomeMediaDismissed = onHomeMediaDismissed,
                        onSettingsSelected = onSettingsSelected,
                        onFocusStart = { pendingFocusMinutes = it },
                        onFocusPause = onFocusPause,
                        onFocusResume = onFocusResume,
                        onFocusFinish = onFocusFinish,
                        onAppSelected = onAppSelected,
                        onAppLongPressed = { appWithOpenActions = it },
                        onHomeButtonTap = onHomeButtonTap,
                        onHomeButtonLongPress = onHomeButtonLongPress,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 48.dp,
                                bottom = if (
                                    WorkspaceDataPolicy.showsContextDock(renderedContext.definition.kind)
                                ) {
                                    88.dp
                                } else {
                                    16.dp
                                },
                            ),
                    )
                }
            }
        }

        TopBar(
            contexts = state.contexts.map { it.definition },
            activeContextIndex = state.activeContextIndex,
            onContextSelected = onContextSelected,
            systemStatus = state.systemStatus,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        AnimatedVisibility(
            visible = showsContextDock,
            enter = fadeIn(
                tween(
                    VeilMotion.StandardDurationMillis,
                    delayMillis = 20,
                    easing = VeilMotion.enterEasing,
                ),
            ) + slideInVertically(
                tween(
                    VeilMotion.EmphasizedDurationMillis,
                    easing = VeilMotion.standardEasing,
                ),
                initialOffsetY = { it },
            ),
            exit = fadeOut(
                tween(
                    VeilMotion.QuickDurationMillis,
                    easing = VeilMotion.exitEasing,
                ),
            ) + slideOutVertically(
                tween(
                    VeilMotion.StandardDurationMillis,
                    easing = VeilMotion.exitEasing,
                ),
                targetOffsetY = { it },
            ),
            label = "context dock visibility",
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            AnimatedContent(
                targetState = displayedDockContextIndex,
                transitionSpec = {
                    fadeIn(
                        tween(
                            VeilMotion.StandardDurationMillis,
                            easing = VeilMotion.enterEasing,
                        ),
                    ).togetherWith(
                        fadeOut(
                            tween(
                                VeilMotion.QuickDurationMillis,
                                easing = VeilMotion.exitEasing,
                            ),
                        ),
                    )
                },
                contentKey = { it },
                label = "context dock content",
                modifier = Modifier.fillMaxWidth(),
            ) { contextIndex ->
                state.contexts.getOrNull(contextIndex)?.let { dockContext ->
                    ContextDock(
                        actions = dockContext.quickActions,
                        settingsShortcuts = settingsShortcuts,
                        onAppSelected = onAppSelected,
                        onAppLongPressed = { appWithOpenActions = it },
                        onSettingSelected = onSettingsSelected,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.isDrawerOpen,
            enter = fadeIn(
                animationSpec = tween(
                    VeilMotion.StandardDurationMillis,
                    easing = VeilMotion.enterEasing,
                ),
                initialAlpha = 0.72f,
            ) + slideInVertically(
                animationSpec = tween(
                    VeilMotion.EmphasizedDurationMillis,
                    easing = VeilMotion.standardEasing,
                ),
                initialOffsetY = { it / 6 },
            ),
            exit = fadeOut(
                animationSpec = tween(
                    VeilMotion.QuickDurationMillis,
                    easing = VeilMotion.exitEasing,
                ),
            ) + slideOutVertically(
                animationSpec = tween(
                    VeilMotion.StandardDurationMillis,
                    easing = VeilMotion.exitEasing,
                ),
                targetOffsetY = { it / 8 },
            ),
            label = "app drawer",
        ) {
            AppDrawer(
                installedApps = state.installedApps,
                settingsShortcuts = settingsShortcuts,
                isLoading = state.isLoading,
                onAppSelected = onAppSelected,
                onAppLongPressed = { appWithOpenActions = it },
                onSettingsSelected = onSettingsSelected,
                continuityAccessGranted = state.continuityAccessGranted,
                onContinuityAccessSelected = onContinuityAccessRequested,
                onClose = onCloseDrawer,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    BackHandler(enabled = state.isDrawerOpen, onBack = onCloseDrawer)

    if (pendingFocusMinutes > 0) {
        AlertDialog(
            onDismissRequest = { pendingFocusMinutes = 0 },
            title = { Text("Focus fiable") },
            text = {
                Text(
                    "Veil iniciará una sesión de $pendingFocusMinutes minutos. Para avisarte incluso " +
                        "con la pantalla apagada puede solicitar notificaciones y acceso a alarmas exactas.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = pendingFocusMinutes
                    pendingFocusMinutes = 0
                    onFocusStartRequested(minutes)
                }) { Text("Iniciar") }
            },
            dismissButton = {
                TextButton(onClick = { pendingFocusMinutes = 0 }) { Text("Cancelar") }
            },
        )
    }

    if (showLocationDisclosure) {
        AlertDialog(
            onDismissRequest = { showLocationDisclosure = false },
            title = { Text("Tiempo local") },
            text = {
                Text(
                    "Veil usará únicamente ubicación aproximada mientras Home esté visible. " +
                        "Las coordenadas aproximadas y tu IP se enviarán a Open‑Meteo; " +
                        "Veil guardará sólo el último resultado durante la caché.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLocationDisclosure = false
                    onLocationPermissionRequested()
                }) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDisclosure = false }) { Text("Cancelar") }
            },
        )
    }

    appWithOpenActions?.let { app ->
        AppActionsBottomSheet(
            app = app,
            onDismiss = { appWithOpenActions = null },
            onOpen = { appWithOpenActions = null; onAppSelected(app) },
            onAppInfo = { appWithOpenActions = null; onAppInfoSelected(app) },
            onUninstall = { appWithOpenActions = null; onAppUninstallSelected(app) },
        )
    }
}
