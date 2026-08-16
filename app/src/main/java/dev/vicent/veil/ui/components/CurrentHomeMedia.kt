package dev.vicent.veil.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.ui.theme.LocalVeilPalette
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun CompactMediaPlayer(
    media: ContinuityItem.Media,
    onAction: (String, ContinuityAction, Long?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    var horizontalOffset by remember(media.id) { mutableFloatStateOf(0f) }
    val shape = RoundedCornerShape(10.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .offset { IntOffset(horizontalOffset.roundToInt(), 0) }
            .widthIn(max = 302.dp)
            .fillMaxWidth()
            .height(62.dp)
            .clip(shape)
            .background(Color(0xFF0D1114).copy(alpha = 0.82f))
            .border(1.dp, palette.divider, shape)
            .pointerInput(media.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        horizontalOffset += amount
                    },
                    onDragEnd = {
                        if (abs(horizontalOffset) > size.width * .24f) onDismiss()
                        else horizontalOffset = 0f
                    },
                    onDragCancel = { horizontalOffset = 0f },
                )
            }
            .padding(start = 8.dp, end = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(
                    enabled = ContinuityAction.OPEN in media.supportedActions,
                    role = Role.Button,
                    onClickLabel = "Abrir ${media.appLabel}",
                ) { onAction(media.id, ContinuityAction.OPEN, null) },
        ) {
            if (media.artwork != null) {
                Image(
                    bitmap = remember(media.artwork) { media.artwork.asImageBitmap() },
                    contentDescription = null,
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(7.dp)),
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(palette.subtleFill),
                ) {
                    ActivityGlyph(ActivityGlyphKind.MEDIA, size = 22.dp, isActive = true)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 9.dp, end = 4.dp)) {
                BasicText(
                    media.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.contentPrimary,
                        fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                BasicText(
                    media.subtitle ?: media.appLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.contentMuted,
                        fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                        fontSize = 9.sp,
                    ),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (ContinuityAction.SKIP_PREVIOUS in media.supportedActions) {
                CompactMediaControl(
                    CompactMediaControlKind.PREVIOUS,
                    "Canción anterior",
                ) { onAction(media.id, ContinuityAction.SKIP_PREVIOUS, null) }
            }
            if (ContinuityAction.TOGGLE_PLAYBACK in media.supportedActions) {
                CompactMediaControl(
                    if (media.isPlaying) CompactMediaControlKind.PAUSE else CompactMediaControlKind.PLAY,
                    if (media.isPlaying) "Pausar" else "Reproducir",
                ) { onAction(media.id, ContinuityAction.TOGGLE_PLAYBACK, null) }
            }
            if (ContinuityAction.SKIP_NEXT in media.supportedActions) {
                CompactMediaControl(
                    CompactMediaControlKind.NEXT,
                    "Canción siguiente",
                ) { onAction(media.id, ContinuityAction.SKIP_NEXT, null) }
            }
        }
    }
}

private enum class CompactMediaControlKind { PREVIOUS, PLAY, PAUSE, NEXT }

@Composable
private fun CompactMediaControl(
    kind: CompactMediaControlKind,
    label: String,
    onClick: () -> Unit,
) {
    val color = LocalVeilPalette.current.contentPrimary
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
    ) {
        Canvas(Modifier.size(16.dp)) {
            val strokeWidth = 1.6.dp.toPx()
            when (kind) {
                CompactMediaControlKind.PREVIOUS,
                CompactMediaControlKind.NEXT,
                -> {
                    val pointsLeft = kind == CompactMediaControlKind.PREVIOUS
                    val barX = if (pointsLeft) size.width * .18f else size.width * .82f
                    drawLine(
                        color,
                        Offset(barX, size.height * .18f),
                        Offset(barX, size.height * .82f),
                        strokeWidth,
                        StrokeCap.Round,
                    )
                    val path = Path().apply {
                        if (pointsLeft) {
                            moveTo(size.width * .72f, size.height * .17f)
                            lineTo(size.width * .28f, size.height * .5f)
                            lineTo(size.width * .72f, size.height * .83f)
                        } else {
                            moveTo(size.width * .28f, size.height * .17f)
                            lineTo(size.width * .72f, size.height * .5f)
                            lineTo(size.width * .28f, size.height * .83f)
                        }
                        close()
                    }
                    drawPath(path, color)
                }
                CompactMediaControlKind.PLAY -> {
                    val path = Path().apply {
                        moveTo(size.width * .28f, size.height * .14f)
                        lineTo(size.width * .78f, size.height * .5f)
                        lineTo(size.width * .28f, size.height * .86f)
                        close()
                    }
                    drawPath(path, color)
                }
                CompactMediaControlKind.PAUSE -> {
                    drawRoundRect(
                        color,
                        Offset(size.width * .23f, size.height * .15f),
                        androidx.compose.ui.geometry.Size(size.width * .18f, size.height * .7f),
                        androidx.compose.ui.geometry.CornerRadius(strokeWidth / 2f),
                    )
                    drawRoundRect(
                        color,
                        Offset(size.width * .59f, size.height * .15f),
                        androidx.compose.ui.geometry.Size(size.width * .18f, size.height * .7f),
                        androidx.compose.ui.geometry.CornerRadius(strokeWidth / 2f),
                    )
                }
            }
        }
    }
}
