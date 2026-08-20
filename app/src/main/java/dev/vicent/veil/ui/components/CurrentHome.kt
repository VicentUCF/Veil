package dev.vicent.veil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.R
import dev.vicent.veil.launcher.ResolvedQuickAction
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
internal fun CurrentHome(
    state: CurrentHomeUiState,
    actions: List<ResolvedQuickAction>,
    onAppSelected: (LauncherApp) -> Unit,
    onAppLongPressed: (LauncherApp) -> Unit,
    onEmptySlotSelected: (Int) -> Unit,
    onLocationPermissionRequested: () -> Unit,
    onClockOpenRequested: () -> Unit,
    onCalendarOpenRequested: () -> Unit,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
    onMediaDismissed: (String) -> Unit,
    onQuickButtonTap: () -> Unit,
    onQuickButtonLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playingMedia = state.mediaContinuity?.takeIf { it.isPlaying }
    var dismissedMediaId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(playingMedia?.id) {
        when {
            playingMedia == null -> dismissedMediaId = null
            dismissedMediaId != null && dismissedMediaId != playingMedia.id -> dismissedMediaId = null
        }
    }

    val homeAppearance = resolveCurrentHomeAppearance(
        tone = state.preferences.homeTextTone,
        weight = state.preferences.homeTextWeight,
    )
    CompositionLocalProvider(LocalCurrentHomeAppearance provides homeAppearance) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val upperBreathingRoom = (maxHeight * 0.32f).coerceIn(130.dp, 280.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(upperBreathingRoom))
            HomeClockAndWeather(
                state = state,
                onLocationPermissionRequested = onLocationPermissionRequested,
                onClockOpenRequested = onClockOpenRequested,
                onCalendarOpenRequested = onCalendarOpenRequested,
            )

            if (playingMedia != null && dismissedMediaId != playingMedia.id) {
                CompactMediaPlayer(
                    media = playingMedia,
                    onAction = onContinuityAction,
                    onDismiss = {
                        dismissedMediaId = playingMedia.id
                        onMediaDismissed(playingMedia.id)
                    },
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.padding(top = 10.dp).widthIn(max = 278.dp),
            ) {
                actions.take(5).forEachIndexed { index, action ->
                    val app = (action as? ResolvedQuickAction.App)?.app
                    if (app != null) {
                        HomeAppRow(
                            app = app,
                            hasNotification = app.packageName in state.notificationIndicatorPackages,
                            onClick = { onAppSelected(app) },
                            onLongClick = { onAppLongPressed(app) },
                        )
                    } else {
                        EmptyHomeAppRow(onClick = { onEmptySlotSelected(index) })
                    }
                }
            }
        }

            HomeQuickButton(
                onClick = onQuickButtonTap,
                onLongClick = onQuickButtonLongPress,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 12.dp),
            )
        }
    }
}

@Composable
private fun EmptyHomeAppRow(onClick: () -> Unit) {
    val homeAppearance = LocalCurrentHomeAppearance.current
    val chooseAppLabel = stringResource(R.string.current_choose_empty_app)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(49.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = chooseAppLabel,
                onClick = onClick,
            ),
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.width(39.dp)) {
            BasicText("+", style = homeSmallMonoStyle(homeAppearance.muted))
        }
        BasicText(
            text = stringResource(R.string.current_choose_app),
            style = TextStyle(
                color = homeAppearance.muted,
                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                fontWeight = homeAppearance.contentWeight,
                fontSize = 12.sp,
                letterSpacing = 3.4.sp,
            ),
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}


@Composable
private fun HomeAppRow(
    app: LauncherApp,
    hasNotification: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val homeAppearance = LocalCurrentHomeAppearance.current
    val openLabel = stringResource(R.string.action_open_named, app.label)
    val optionsLabel = stringResource(R.string.action_options_named, app.label)
    val notificationState = stringResource(R.string.state_has_notifications)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(49.dp)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = openLabel,
                onLongClickLabel = optionsLabel,
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .then(
                if (hasNotification) {
                    Modifier.semantics {
                        stateDescription = notificationState
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.width(39.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(31.dp)) {
                ActivityGlyph(
                    kind = app.activityGlyph(),
                    size = 27.dp,
                    color = homeAppearance.secondary,
                )
                AppNotificationIndicator(
                    visible = hasNotification,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
        BasicText(
            text = app.label.uppercase(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = homeAppearance.primary,
                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                fontWeight = homeAppearance.contentWeight,
                fontSize = 13.sp,
                letterSpacing = 4.2.sp,
            ),
            modifier = Modifier.padding(start = 5.dp),
        )
    }
}

@Composable
private fun HomeQuickButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    val homeAppearance = LocalCurrentHomeAppearance.current
    val quickActionLabel = stringResource(R.string.current_quick_action)
    val secondaryQuickActionLabel = stringResource(R.string.current_secondary_quick_action)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(homeAppearance.quickButtonBackground.copy(alpha = .88f))
            .border(1.dp, homeAppearance.secondary, CircleShape)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = quickActionLabel,
                onLongClickLabel = secondaryQuickActionLabel,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        VeilGlyph(
            color = palette.accentActive,
            modifier = Modifier.size(width = 24.dp, height = 25.dp),
        )
    }
}
