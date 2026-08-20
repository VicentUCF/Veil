package dev.vicent.veil.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.FocusTimerState
import dev.vicent.veil.launcher.model.FocusTimerStatus
import dev.vicent.veil.ui.theme.LocalVeilPalette
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun WorkPomodoroTile(
    focus: FocusTimerState,
    compact: Boolean,
    onStart: (Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
) {
    var customMinutes by remember { mutableIntStateOf(25) }
    var showDialog by remember { mutableStateOf(false) }
    CozyTile(
        label = stringResource(R.string.focus_tile_label),
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth().heightIn(
            min = if (compact) 112.dp else WorkspaceLayoutTokens.SECONDARY_TILE_HEIGHT,
        ),
    ) {
        PomodoroDial(focus)
    }

    if (showDialog) {
        PomodoroDialog(
            focus = focus,
            customMinutes = customMinutes,
            onCustomMinutesChanged = { customMinutes = it.coerceIn(5, 180) },
            onDismiss = { showDialog = false },
            onStart = { minutes -> showDialog = false; onStart(minutes) },
            onPause = { showDialog = false; onPause() },
            onResume = { showDialog = false; onResume() },
            onFinish = { showDialog = false; onFinish() },
        )
    }
}

@Composable
private fun PomodoroDial(focus: dev.vicent.veil.launcher.model.FocusTimerState) {
    val palette = LocalVeilPalette.current
    val progress = when (focus.status) {
        FocusTimerStatus.IDLE -> 1f
        else -> if (focus.durationMillis > 0L) {
            (focus.remainingMillis.toFloat() / focus.durationMillis).coerceIn(0f, 1f)
        } else 0f
    }
    val time = when (focus.status) {
        FocusTimerStatus.IDLE -> stringResource(R.string.focus_default_short)
        FocusTimerStatus.COMPLETED -> stringResource(R.string.focus_zero_time)
        else -> formatDuration(focus.remainingMillis)
    }
    val status = when (focus.status) {
        FocusTimerStatus.IDLE -> stringResource(R.string.focus_status_choose)
        FocusTimerStatus.RUNNING -> stringResource(R.string.focus_status_running)
        FocusTimerStatus.PAUSED -> stringResource(R.string.focus_status_paused)
        FocusTimerStatus.COMPLETED -> stringResource(R.string.focus_status_completed)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(78.dp)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val strokeWidth = 2.5.dp.toPx()
                val centre = Offset(size.width / 2f, size.height / 2f + 5.dp.toPx())
                val radius = 29.dp.toPx()
                val arcSize = radius * 2f
                val arcTopLeft = Offset(centre.x - radius, centre.y - radius)

                // Crown, neck and side pusher give the silhouette of a physical timer.
                drawRoundRect(
                    color = palette.contentMuted,
                    topLeft = Offset(centre.x - 8.dp.toPx(), 0f),
                    size = Size(16.dp.toPx(), 5.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
                drawRoundRect(
                    color = palette.contentMuted,
                    topLeft = Offset(centre.x - 3.dp.toPx(), 4.dp.toPx()),
                    size = Size(6.dp.toPx(), 6.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
                drawLine(
                    color = palette.contentMuted,
                    start = Offset(centre.x + 21.dp.toPx(), centre.y - 22.dp.toPx()),
                    end = Offset(centre.x + 27.dp.toPx(), centre.y - 28.dp.toPx()),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = palette.contentMuted,
                    start = Offset(centre.x + 24.dp.toPx(), centre.y - 27.dp.toPx()),
                    end = Offset(centre.x + 29.dp.toPx(), centre.y - 22.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                drawCircle(
                    color = palette.fieldBackground.copy(alpha = 0.92f),
                    radius = radius,
                    center = centre,
                )
                drawArc(
                    color = palette.divider,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(strokeWidth),
                )
                repeat(12) { index ->
                    val angle = Math.toRadians((index * 30 - 90).toDouble())
                    val outer = radius - 5.dp.toPx()
                    val inner = outer - if (index % 3 == 0) 5.dp.toPx() else 3.dp.toPx()
                    drawLine(
                        color = if (index % 3 == 0) {
                            palette.contentSecondary
                        } else {
                            palette.contentMuted.copy(alpha = 0.75f)
                        },
                        start = Offset(
                            centre.x + cos(angle).toFloat() * inner,
                            centre.y + sin(angle).toFloat() * inner,
                        ),
                        end = Offset(
                            centre.x + cos(angle).toFloat() * outer,
                            centre.y + sin(angle).toFloat() * outer,
                        ),
                        strokeWidth = if (index % 3 == 0) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
                drawArc(
                    color = palette.accentActive,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
                )
                val handAngle = Math.toRadians((-90f + 360f * progress).toDouble())
                drawLine(
                    color = palette.accentActive,
                    start = centre,
                    end = Offset(
                        centre.x + cos(handAngle).toFloat() * 12.dp.toPx(),
                        centre.y + sin(handAngle).toFloat() * 12.dp.toPx(),
                    ),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(palette.contentPrimary, 2.5.dp.toPx(), centre)
            }
            BasicText(
                time,
                style = workspaceMonoStyle(palette.contentPrimary, 9),
                modifier = Modifier
                    .padding(top = 28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(palette.fieldBackground.copy(alpha = 0.96f))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
        BasicText(
            status.uppercase(),
            style = workspaceMonoStyle(palette.contentSecondary, 8),
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun PomodoroDialog(
    focus: dev.vicent.veil.launcher.model.FocusTimerState,
    customMinutes: Int,
    onCustomMinutesChanged: (Int) -> Unit,
    onDismiss: () -> Unit,
    onStart: (Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
) {
    val title = when (focus.status) {
        FocusTimerStatus.IDLE -> stringResource(R.string.focus_dialog_new)
        FocusTimerStatus.RUNNING -> stringResource(R.string.focus_dialog_running)
        FocusTimerStatus.PAUSED -> stringResource(R.string.focus_dialog_paused)
        FocusTimerStatus.COMPLETED -> stringResource(R.string.focus_dialog_completed)
    }
    RofiDialog(
        title = title,
        onDismiss = onDismiss,
        actions = {
            if (focus.status == FocusTimerStatus.RUNNING ||
                focus.status == FocusTimerStatus.PAUSED
            ) {
                RofiAction(stringResource(R.string.focus_finish), onFinish, danger = true)
            }
            Spacer(Modifier.weight(1f))
            RofiAction(stringResource(R.string.action_cancel), onDismiss)
            when (focus.status) {
                FocusTimerStatus.IDLE -> RofiAction(
                    stringResource(R.string.focus_start),
                    { onStart(customMinutes) },
                )
                FocusTimerStatus.RUNNING ->
                    RofiAction(stringResource(R.string.focus_pause), onPause)
                FocusTimerStatus.PAUSED ->
                    RofiAction(stringResource(R.string.focus_resume), onResume)
                FocusTimerStatus.COMPLETED ->
                    RofiAction(stringResource(R.string.action_close), onFinish)
            }
        },
    ) {
        when (focus.status) {
            FocusTimerStatus.IDLE -> {
                BasicText(
                    stringResource(R.string.focus_duration),
                    style = workspaceMonoStyle(LocalVeilPalette.current.accentActive, 9),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RofiAction(
                        pluralStringResource(R.plurals.focus_minutes, 25, 25),
                        { onCustomMinutesChanged(25) },
                    )
                    RofiAction(
                        pluralStringResource(R.plurals.focus_minutes, 50, 50),
                        { onCustomMinutesChanged(50) },
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RofiAction(
                        stringResource(R.string.focus_decrease_five),
                        { onCustomMinutesChanged(customMinutes - 5) },
                    )
                    BasicText(
                        pluralStringResource(
                            R.plurals.focus_minutes,
                            customMinutes,
                            customMinutes,
                        ),
                        style = workspaceMonoStyle(LocalVeilPalette.current.contentPrimary, 18),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    RofiAction(
                        stringResource(R.string.focus_increase_five),
                        { onCustomMinutesChanged(customMinutes + 5) },
                    )
                }
                RofiBody(stringResource(R.string.focus_permission_explanation))
            }
            FocusTimerStatus.RUNNING, FocusTimerStatus.PAUSED -> {
                BasicText(
                    formatDuration(focus.remainingMillis),
                    style = workspaceMonoStyle(LocalVeilPalette.current.contentPrimary, 28),
                )
                if (!focus.exactAlarmAvailable || !focus.notificationsAvailable) {
                    RofiBody(stringResource(R.string.focus_limited_notice))
                }
            }
            FocusTimerStatus.COMPLETED -> RofiBody(
                stringResource(R.string.focus_completed_notice),
            )
        }
    }
}
