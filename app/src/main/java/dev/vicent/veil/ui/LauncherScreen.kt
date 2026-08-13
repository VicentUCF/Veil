package dev.vicent.veil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.vicent.veil.launcher.LauncherUiState
import dev.vicent.veil.launcher.WorkspaceDataPolicy
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.AudioChannel
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.QuickNoteChecklistItem
import dev.vicent.veil.launcher.model.QuickNoteType
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.ui.components.AppActionsBottomSheet
import dev.vicent.veil.ui.components.AppDrawer
import dev.vicent.veil.ui.components.ContextDock
import dev.vicent.veil.ui.components.RofiAction
import dev.vicent.veil.ui.components.RofiBody
import dev.vicent.veil.ui.components.RofiDialog
import dev.vicent.veil.ui.components.TopBar
import dev.vicent.veil.ui.components.WorkspaceDashboard
import dev.vicent.veil.ui.theme.VeilMotion
import kotlinx.coroutines.launch

@Composable
fun LauncherScreen(
    state: LauncherUiState,
    settingsShortcuts: List<SettingsShortcut>,
    onContextSelected: (Int) -> Unit,
    onOpenDrawer: () -> Unit,
    onCloseDrawer: () -> Unit,
    onAppSelected: (LauncherApp) -> Unit,
    onSettingsSelected: (SettingsShortcut) -> Unit,
    onAppInfoSelected: (LauncherApp) -> Unit,
    onAppUninstallSelected: (LauncherApp) -> Unit,
    onContinuityAccessRequested: () -> Unit,
    onCalendarPermissionRequested: () -> Unit,
    onLocationPermissionRequested: () -> Unit,
    onClockOpenRequested: () -> Unit,
    onCalendarEventSelected: (Long) -> Unit,
    onCalendarEventCreateRequested: () -> Unit,
    onCalendarOpenRequested: () -> Unit,
    onGoogleCalendarConfigureRequested: () -> Unit,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
    onHomeMediaDismissed: (String) -> Unit,
    onAudioVisualizerPermissionRequested: () -> Unit,
    onAudioVolumeChanged: (AudioChannel, Float) -> Unit,
    onFocusStartRequested: (Int) -> Unit,
    onFocusPause: () -> Unit,
    onFocusResume: () -> Unit,
    onFocusFinish: () -> Unit,
    onQuickNoteAdded: (String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onQuickNoteUpdated: (Long, String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onQuickNoteDeleted: (Long) -> Unit,
    onHomeButtonTap: () -> Unit,
    onHomeButtonLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var appWithOpenActions by remember { mutableStateOf<LauncherApp?>(null) }
    var showLocationDisclosure by remember { mutableStateOf(false) }
    var showAudioVisualizerDisclosure by remember { mutableStateOf(false) }

    LaunchedEffect(state.isDrawerOpen) {
        if (state.isDrawerOpen) appWithOpenActions = null
    }

    val contextCount = state.contexts.size
    val gesturesEnabled = !state.isDrawerOpen
    val pagerState = if (contextCount > 0) {
        rememberPagerState(
            initialPage = canonicalPagerPage(
                contextIndex = state.activeContextIndex.coerceIn(0, contextCount - 1),
                contextCount = contextCount,
            ),
            pageCount = { circularPagerPageCount(contextCount) },
        )
    } else {
        null
    }
    val pagerSnapAnimationSpec = remember {
        tween<Float>(
            durationMillis = VeilMotion.StandardDurationMillis,
            easing = VeilMotion.standardEasing,
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val currentOnContextSelected by rememberUpdatedState(onContextSelected)
    var lastReportedContextIndex by remember(contextCount) {
        mutableIntStateOf(state.activeContextIndex)
    }

    LaunchedEffect(pagerState, contextCount) {
        if (pagerState == null || contextCount == 0) return@LaunchedEffect
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            val settledContextIndex = contextIndexForPagerPage(settledPage, contextCount)
            if (settledContextIndex != lastReportedContextIndex) {
                lastReportedContextIndex = settledContextIndex
                currentOnContextSelected(settledContextIndex)
            }

            val canonicalPage = canonicalPageForBoundary(settledPage, contextCount)
            if (canonicalPage != settledPage) pagerState.scrollToPage(canonicalPage)
        }
    }

    LaunchedEffect(state.activeContextIndex, contextCount) {
        if (pagerState == null || contextCount == 0 || pagerState.isScrollInProgress) {
            return@LaunchedEffect
        }
        val activeContextIndex = state.activeContextIndex.coerceIn(0, contextCount - 1)
        lastReportedContextIndex = activeContextIndex
        if (contextIndexForPagerPage(pagerState.settledPage, contextCount) != activeContextIndex) {
            pagerState.scrollToPage(canonicalPagerPage(activeContextIndex, contextCount))
        }
    }

    val homeGestureModifier = if (gesturesEnabled) {
        Modifier.pointerInput(Unit) {
            var verticalDistance = 0f
            val threshold = 72.dp.toPx()
            detectVerticalDragGestures(
                onDragStart = { verticalDistance = 0f },
                onVerticalDrag = { change, dragAmount ->
                    if (!change.isConsumed) {
                        verticalDistance += dragAmount
                        if (verticalDistance <= -threshold) change.consume()
                    }
                },
                onDragEnd = {
                    if (verticalDistance <= -threshold) onOpenDrawer()
                },
                onDragCancel = { verticalDistance = 0f },
            )
        }
    } else Modifier

    Box(modifier = modifier.fillMaxSize().then(homeGestureModifier)) {
        if (pagerState != null) {
            val flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                pagerSnapDistance = PagerSnapDistance.atMost(1),
                snapAnimationSpec = pagerSnapAnimationSpec,
                snapPositionalThreshold = 0.15f,
            )
            HorizontalPager(
                state = pagerState,
                flingBehavior = flingBehavior,
                userScrollEnabled = gesturesEnabled,
                key = { page -> page },
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val contextIndex = contextIndexForPagerPage(page, contextCount)
                state.contexts.getOrNull(contextIndex)?.let { renderedContext ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(
                                WindowInsets.navigationBars.only(WindowInsetsSides.Bottom),
                            ),
                    ) {
                        WorkspaceDashboard(
                            state = state,
                            context = renderedContext,
                            settingsShortcuts = settingsShortcuts,
                            onCalendarPermissionRequested = onCalendarPermissionRequested,
                            onLocationPermissionRequested = { showLocationDisclosure = true },
                            onClockOpenRequested = onClockOpenRequested,
                            onContinuityAccessRequested = onContinuityAccessRequested,
                            onCalendarEventSelected = onCalendarEventSelected,
                            onCalendarEventCreateRequested = onCalendarEventCreateRequested,
                            onCalendarOpenRequested = onCalendarOpenRequested,
                            onGoogleCalendarConfigureRequested = onGoogleCalendarConfigureRequested,
                            onContinuityAction = onContinuityAction,
                            onHomeMediaDismissed = onHomeMediaDismissed,
                            onAudioVisualizerPermissionRequested = {
                                showAudioVisualizerDisclosure = true
                            },
                            onAudioVolumeChanged = onAudioVolumeChanged,
                            onSettingsSelected = onSettingsSelected,
                            onFocusStart = onFocusStartRequested,
                            onFocusPause = onFocusPause,
                            onFocusResume = onFocusResume,
                            onFocusFinish = onFocusFinish,
                            onQuickNoteAdded = onQuickNoteAdded,
                            onQuickNoteUpdated = onQuickNoteUpdated,
                            onQuickNoteDeleted = onQuickNoteDeleted,
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
                                        WorkspaceDataPolicy.showsContextDock(
                                            renderedContext.definition.kind,
                                        )
                                    ) {
                                        88.dp
                                    } else {
                                        16.dp
                                    },
                                ),
                        )

                        if (WorkspaceDataPolicy.showsContextDock(renderedContext.definition.kind)) {
                            ContextDock(
                                actions = renderedContext.quickActions,
                                settingsShortcuts = settingsShortcuts,
                                onAppSelected = onAppSelected,
                                onAppLongPressed = { appWithOpenActions = it },
                                onSettingSelected = onSettingsSelected,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                    }
                }
            }
        }

        val visibleContextIndex = if (pagerState != null && contextCount > 0) {
            contextIndexForPagerPage(pagerState.currentPage, contextCount)
        } else {
            state.activeContextIndex
        }
        TopBar(
            contexts = state.contexts.map { it.definition },
            activeContextIndex = visibleContextIndex,
            onContextSelected = { targetContextIndex ->
                if (pagerState != null && targetContextIndex in 0 until contextCount) {
                    coroutineScope.launch {
                        val sourceContextIndex = contextIndexForPagerPage(
                            pagerState.currentPage,
                            contextCount,
                        )
                        circularStepDirections(
                            fromContextIndex = sourceContextIndex,
                            toContextIndex = targetContextIndex,
                            contextCount = contextCount,
                        ).forEach { direction ->
                            pagerState.animateScrollToPage(
                                page = pagerState.currentPage + direction,
                                animationSpec = pagerSnapAnimationSpec,
                            )
                            val settledPage = pagerState.settledPage
                            val canonicalPage = canonicalPageForBoundary(
                                settledPage,
                                contextCount,
                            )
                            if (canonicalPage != settledPage) {
                                pagerState.scrollToPage(canonicalPage)
                            }
                        }
                    }
                }
            },
            systemStatus = state.systemStatus,
            onClockOpenRequested = onClockOpenRequested,
            modifier = Modifier.align(Alignment.TopCenter),
        )

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

    if (showLocationDisclosure) {
        RofiDialog(
            title = "tiempo local",
            onDismiss = { showLocationDisclosure = false },
            actions = {
                RofiAction("cancelar", { showLocationDisclosure = false })
                RofiAction("continuar", {
                    showLocationDisclosure = false
                    onLocationPermissionRequested()
                })
            },
        ) {
            RofiBody(
                "Veil usará únicamente ubicación aproximada mientras Home esté visible. " +
                    "Las coordenadas aproximadas y tu IP se enviarán a Open‑Meteo; " +
                    "Veil guardará sólo el último resultado durante la caché.",
            )
        }
    }

    if (showAudioVisualizerDisclosure) {
        RofiDialog(
            title = "espectro de audio",
            onDismiss = { showAudioVisualizerDisclosure = false },
            actions = {
                RofiAction("ahora no", { showAudioVisualizerDisclosure = false })
                RofiAction("activar", {
                    showAudioVisualizerDisclosure = false
                    onAudioVisualizerPermissionRequested()
                })
            },
        ) {
            RofiBody(
                "Android exige permiso de micrófono para analizar la mezcla de salida. " +
                    "Veil sólo recibe una señal FFT de baja calidad mientras MEDIA está visible; " +
                    "no graba, guarda ni transmite audio.",
            )
        }
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
