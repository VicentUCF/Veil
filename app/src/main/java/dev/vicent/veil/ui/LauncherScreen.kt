package dev.vicent.veil.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import dev.vicent.veil.launcher.LauncherUiState
import dev.vicent.veil.launcher.WorkspaceLayoutPolicy
import dev.vicent.veil.launcher.model.HomeTextTone
import dev.vicent.veil.launcher.model.WallpaperScrimPolicy
import dev.vicent.veil.launcher.model.LauncherSurface
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.ui.components.ContextDock
import dev.vicent.veil.ui.components.LauncherPublisherInfo
import dev.vicent.veil.ui.components.TopBar
import dev.vicent.veil.ui.components.WorkspaceDashboard
import dev.vicent.veil.ui.components.WorkspaceSettingsScreen
import dev.vicent.veil.ui.theme.VeilMotion
import kotlinx.coroutines.launch

internal enum class LauncherBackAction {
    KEEP_HOME,
    CLOSE_EVERYTHING,
    CLOSE_SETTINGS_DETAIL,
    CLOSE_SETTINGS,
}

internal fun launcherBackAction(
    surface: LauncherSurface,
    isSettingsDetailOpen: Boolean,
): LauncherBackAction = when (surface) {
    LauncherSurface.HOME -> LauncherBackAction.KEEP_HOME
    LauncherSurface.EVERYTHING -> LauncherBackAction.CLOSE_EVERYTHING
    LauncherSurface.SETTINGS -> if (isSettingsDetailOpen) {
        LauncherBackAction.CLOSE_SETTINGS_DETAIL
    } else {
        LauncherBackAction.CLOSE_SETTINGS
    }
}

@Composable
fun LauncherScreen(
    state: LauncherUiState,
    systemAccent: Color?,
    publisherInfo: LauncherPublisherInfo,
    settingsShortcuts: List<SettingsShortcut>,
    navigationActions: LauncherNavigationActions,
    appActions: LauncherAppActions,
    accessActions: LauncherAccessActions,
    workspaceActions: LauncherWorkspaceActions,
    appearanceActions: LauncherAppearanceActions,
    catalogActions: LauncherCatalogActions,
    modifier: Modifier = Modifier,
) {
    val (
        onContextSelected,
        onOpenDrawer,
        onCloseDrawer,
        onOpenSettings,
        onCloseSettings,
        onOpenMusicProviderPicker,
        onOpenHomeButtonPicker,
        onOpenContextSlotPicker,
        onHomeButtonTap,
        onHomeButtonLongPress,
    ) = navigationActions
    val (
        onAppSelected,
        _,
        onSettingsSelected,
        _,
        _,
        onExternalLinkSelected,
        _,
        _,
        _,
        _,
        _,
    ) = appActions
    val (
        onContinuityAccessRequested,
        _,
        onCalendarPermissionRequested,
        _,
        _,
        _,
        _,
        _,
        _,
        _,
        _,
    ) = accessActions
    val (
        onClockOpenRequested,
        onCalendarEventSelected,
        onCalendarEventCreateRequested,
        onCalendarOpenRequested,
        onGoogleCalendarConfigureRequested,
        onContinuityAction,
        onHomeMediaDismissed,
        onAudioVolumeChanged,
        onFocusStartRequested,
        onFocusPause,
        onFocusResume,
        onFocusFinish,
        onQuickNoteAdded,
        onQuickNoteUpdated,
        onQuickNoteDeleted,
    ) = workspaceActions
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var appWithOpenActions by remember { mutableStateOf<AppActionsTarget?>(null) }
    var activeDisclosure by remember { mutableStateOf<LauncherDisclosure?>(null) }
    var showFontSettings by remember { mutableStateOf(false) }
    var showHomeButtonSettings by remember { mutableStateOf(false) }
    var showWorkspaceSettings by remember { mutableStateOf(false) }

    val handleSettingsBack = {
        when {
            showFontSettings -> showFontSettings = false
            showHomeButtonSettings -> showHomeButtonSettings = false
            showWorkspaceSettings -> showWorkspaceSettings = false
            else -> onCloseSettings()
        }
    }
    val handleSettingsSurfaceBack = {
        if (state.settingsAppTarget != null) onCloseSettings() else handleSettingsBack()
    }

    LaunchedEffect(state.isDrawerOpen) {
        if (state.isDrawerOpen) {
            appWithOpenActions = null
        } else {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }
    LaunchedEffect(state.isSettingsOpen) {
        if (!state.isSettingsOpen) {
            showFontSettings = false
            showHomeButtonSettings = false
            showWorkspaceSettings = false
        }
    }

    val contextCount = state.contexts.size
    val gesturesEnabled = state.navigation.surface == LauncherSurface.HOME
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
            durationMillis = VeilMotion.STANDARD_DURATION_MILLIS,
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
        if (state.preferences.wallpaperScrimEnabled) {
            val wallpaperScrim = when (state.preferences.homeTextTone) {
                HomeTextTone.LIGHT -> Color.Black.copy(
                    alpha = WallpaperScrimPolicy.alpha(
                        tone = HomeTextTone.LIGHT,
                        intensity = state.preferences.wallpaperScrimIntensity,
                    ),
                )
                HomeTextTone.DARK -> Color.White.copy(
                    alpha = WallpaperScrimPolicy.alpha(
                        tone = HomeTextTone.DARK,
                        intensity = state.preferences.wallpaperScrimIntensity,
                    ),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(wallpaperScrim),
            )
        }

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
                            onLocationPermissionRequested = {
                                activeDisclosure = LauncherDisclosure.LOCATION
                            },
                            onClockOpenRequested = onClockOpenRequested,
                            onContinuityAccessRequested = { onContinuityAccessRequested() },
                            onCalendarEventSelected = onCalendarEventSelected,
                            onCalendarEventCreateRequested = onCalendarEventCreateRequested,
                            onCalendarOpenRequested = onCalendarOpenRequested,
                            onGoogleCalendarConfigureRequested = onGoogleCalendarConfigureRequested,
                            onContinuityAction = onContinuityAction,
                            onHomeMediaDismissed = onHomeMediaDismissed,
                            onAudioVisualizerPermissionRequested = {
                                activeDisclosure = LauncherDisclosure.AUDIO_VISUALIZER
                            },
                            onAudioVolumeChanged = onAudioVolumeChanged,
                            onSettingsSelected = onSettingsSelected,
                            onVeilSettingsSelected = onOpenSettings,
                            onMusicProviderSelectionRequested = onOpenMusicProviderPicker,
                            onFocusStart = onFocusStartRequested,
                            onFocusPause = onFocusPause,
                            onFocusResume = onFocusResume,
                            onFocusFinish = onFocusFinish,
                            onQuickNoteAdded = onQuickNoteAdded,
                            onQuickNoteUpdated = onQuickNoteUpdated,
                            onQuickNoteDeleted = onQuickNoteDeleted,
                            onExternalLinkSelected = onExternalLinkSelected,
                            onAppSelected = onAppSelected,
                            onAppLongPressed = { app ->
                                appWithOpenActions = AppActionsTarget(
                                    app = app,
                                    contextKind = renderedContext.definition.kind,
                                    slotIndex = renderedContext.quickActions.indexOfFirst { action ->
                                        (action as? dev.vicent.veil.launcher.ResolvedQuickAction.App)
                                            ?.app?.packageName == app.packageName
                                    }.takeIf { it >= 0 },
                                )
                            },
                            onEmptyContextSlotSelected = onOpenContextSlotPicker,
                            onHomeButtonTap = onHomeButtonTap,
                            onHomeButtonLongPress = onHomeButtonLongPress,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 48.dp,
                                    bottom = if (
                                        WorkspaceLayoutPolicy.showsContextDock(
                                            renderedContext.definition.kind,
                                        )
                                    ) {
                                        88.dp
                                    } else {
                                        16.dp
                                    },
                                ),
                        )

                        if (WorkspaceLayoutPolicy.showsContextDock(renderedContext.definition.kind)) {
                            ContextDock(
                                actions = renderedContext.quickActions,
                                notificationIndicatorPackages = state.notificationIndicatorPackages,
                                settingsShortcuts = settingsShortcuts,
                                onAppSelected = onAppSelected,
                                onAppLongPressed = { app ->
                                    appWithOpenActions = AppActionsTarget(
                                        app = app,
                                        contextKind = renderedContext.definition.kind,
                                        slotIndex = renderedContext.quickActions.indexOfFirst { action ->
                                            (action as? dev.vicent.veil.launcher.ResolvedQuickAction.App)
                                                ?.app?.packageName == app.packageName
                                        }.takeIf { it >= 0 },
                                    )
                                },
                                onSettingSelected = onSettingsSelected,
                                onEmptySlotSelected = { slotIndex ->
                                    onOpenContextSlotPicker(
                                        renderedContext.definition.kind,
                                        slotIndex,
                                    )
                                },
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

        LauncherDrawerLayer(
            state = state,
            settingsShortcuts = settingsShortcuts,
            navigationActions = navigationActions,
            appActions = appActions,
            onAppActionsRequested = { appWithOpenActions = it },
            onContinuityDisclosureRequested = {
                activeDisclosure = LauncherDisclosure.CONTINUITY
            },
        )
        LauncherSettingsLayer(
            state = state,
            settingsShortcuts = settingsShortcuts,
            showFontSettings = showFontSettings,
            showHomeButtonSettings = showHomeButtonSettings,
            showWorkspaceSettings = showWorkspaceSettings,
            systemAccent = systemAccent,
            publisherInfo = publisherInfo,
            navigationActions = navigationActions,
            appActions = appActions,
            accessActions = accessActions,
            appearanceActions = appearanceActions,
            catalogActions = catalogActions,
            onBack = handleSettingsSurfaceBack,
            onFontSettingsRequested = { showFontSettings = true },
            onHomeButtonSettingsRequested = { showHomeButtonSettings = true },
            onWorkspaceSettingsRequested = { showWorkspaceSettings = true },
            onDisclosureRequested = { activeDisclosure = it },
        )
    }

    BackHandler {
        when (
            launcherBackAction(
                state.navigation.surface,
                showFontSettings || showHomeButtonSettings || showWorkspaceSettings ||
                    state.settingsAppTarget != null,
            )
        ) {
            LauncherBackAction.KEEP_HOME -> Unit
            LauncherBackAction.CLOSE_EVERYTHING -> onCloseDrawer()
            LauncherBackAction.CLOSE_SETTINGS_DETAIL -> handleSettingsSurfaceBack()
            LauncherBackAction.CLOSE_SETTINGS -> onCloseSettings()
        }
    }

    val showAutomaticContinuityDisclosure =
        state.preferences.workspaceSetupCompleted &&
            !state.continuityAccessGranted && !state.isContinuityOnboardingDismissed
    LauncherOverlays(
        activeDisclosure = activeDisclosure,
        showAutomaticContinuityDisclosure = showAutomaticContinuityDisclosure,
        appActionsTarget = appWithOpenActions,
        navigationActions = navigationActions,
        appActions = appActions,
        accessActions = accessActions,
        onDisclosureDismissed = { activeDisclosure = null },
        onAppActionsDismissed = { appWithOpenActions = null },
    )

    if (!state.preferences.workspaceSetupCompleted) {
        BackHandler { catalogActions.onWorkspaceSetupCompleted() }
        WorkspaceSettingsScreen(
            preferences = state.preferences,
            catalog = state.workspaceCatalog,
            firstRun = true,
            onBack = catalogActions.onWorkspaceSetupCompleted,
            onWorkspaceReplaced = catalogActions.onWorkspaceReplaced,
            onWorkspaceMoved = catalogActions.onWorkspaceMoved,
            onComplete = catalogActions.onWorkspaceSetupCompleted,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
