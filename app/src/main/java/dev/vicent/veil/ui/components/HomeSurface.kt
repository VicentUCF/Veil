package dev.vicent.veil.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
fun ContinuitySurface(
    item: ContinuityItem,
    onAction: (ContinuityAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    val eyebrow = when (item) {
        is ContinuityItem.Media -> if (item.isVideo) {
            stringResource(R.string.continuity_continue_watching)
        } else if (item.isPlaying) {
            stringResource(R.string.continuity_now_playing)
        } else {
            stringResource(R.string.continuity_continue_listening)
        }
        is ContinuityItem.Navigation -> stringResource(R.string.continuity_navigation)
        is ContinuityItem.Progress -> if (item.isComplete) {
            stringResource(R.string.continuity_finished)
        } else {
            stringResource(R.string.continuity_in_progress)
        }
    }
    val glyph = when (item) {
        is ContinuityItem.Media -> ActivityGlyphKind.MEDIA
        is ContinuityItem.Navigation -> ActivityGlyphKind.NAVIGATION
        is ContinuityItem.Progress -> ActivityGlyphKind.PROGRESS
    }

    AccentSurface(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            ActivityGlyph(kind = glyph, size = 22.dp, isActive = true)
            Column(modifier = Modifier.padding(start = 20.dp).widthIn(max = 272.dp)) {
                BasicText(
                    text = eyebrow.uppercase(),
                    style = TextStyle(
                        color = palette.accentActive,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.7.sp,
                    ),
                )
                BasicText(
                    text = item.title,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.contentPrimary,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 25.sp,
                    ),
                    modifier = Modifier.padding(top = 8.dp),
                )
                item.subtitle?.let { subtitle ->
                    BasicText(
                        text = subtitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = palette.contentSecondary,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        ),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                BasicText(
                    text = item.appLabel.uppercase(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.contentMuted,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                    ),
                    modifier = Modifier.padding(top = 9.dp),
                )
                if (item is ContinuityItem.Progress && item.progress != null) {
                    ProgressLine(progress = item.progress, modifier = Modifier.padding(top = 12.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    if (ContinuityAction.TOGGLE_PLAYBACK in item.supportedActions && item is ContinuityItem.Media) {
                        QuietAction(
                            label = if (item.isPlaying) {
                                stringResource(R.string.continuity_pause)
                            } else {
                                stringResource(R.string.continuity_play)
                            },
                            onClick = { onAction(ContinuityAction.TOGGLE_PLAYBACK) },
                        )
                    }
                    if (ContinuityAction.OPEN in item.supportedActions) {
                        QuietAction(
                            label = stringResource(R.string.continuity_resume),
                            onClick = { onAction(ContinuityAction.OPEN) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContinuityOnboardingSurface(
    onEnable: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    AccentSurface(modifier = modifier) {
        Column(modifier = Modifier.widthIn(max = 300.dp)) {
            BasicText(
                text = stringResource(R.string.continuity_onboarding_title),
                style = TextStyle(
                    color = palette.contentPrimary,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            BasicText(
                text = stringResource(R.string.continuity_onboarding_body),
                style = TextStyle(
                    color = palette.contentSecondary,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                ),
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier.padding(top = 16.dp),
            ) {
                QuietAction(stringResource(R.string.continuity_enable), onEnable)
                QuietAction(stringResource(R.string.continuity_not_now), onDismiss)
            }
        }
    }
}

@Composable
fun ToolsSurface(
    shortcuts: List<SettingsShortcut>,
    onSelected: (SettingsShortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    AccentSurface(modifier = modifier) {
        Column {
            shortcuts.take(5).forEach { shortcut ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .widthIn(min = 220.dp, max = 292.dp)
                        .height(54.dp)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = shortcut.label,
                            onClick = { onSelected(shortcut) },
                        )
                        .padding(horizontal = 12.dp),
                ) {
                    ActivityGlyph(kind = ActivityGlyphKind.TOOLS, size = 21.dp)
                    BasicText(
                        text = shortcut.label.uppercase(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            color = palette.contentPrimary,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 14.sp,
                            letterSpacing = 2.2.sp,
                        ),
                        modifier = Modifier.padding(start = 28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccentSurface(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    val palette = LocalVeilPalette.current
    Row(modifier = modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.width(1.dp).fillMaxHeight()) {
            drawLine(palette.accentActive, start = androidx.compose.ui.geometry.Offset.Zero, end = androidx.compose.ui.geometry.Offset(0f, size.height), strokeWidth = 1.dp.toPx())
        }
        Box(modifier = Modifier.padding(start = 18.dp)) { content() }
    }
}

@Composable
private fun QuietAction(label: String, onClick: () -> Unit) {
    val palette = LocalVeilPalette.current
    BasicText(
        text = label.uppercase(),
        style = TextStyle(
            color = palette.contentPrimary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 1.3.sp,
        ),
        modifier = Modifier
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(vertical = 10.dp),
    )
}

@Composable
private fun ProgressLine(progress: Float, modifier: Modifier = Modifier) {
    val palette = LocalVeilPalette.current
    Canvas(modifier = modifier.fillMaxWidth().height(2.dp)) {
        drawLine(palette.divider, start = androidx.compose.ui.geometry.Offset.Zero, end = androidx.compose.ui.geometry.Offset(size.width, 0f), strokeWidth = size.height)
        drawLine(palette.accentActive, start = androidx.compose.ui.geometry.Offset.Zero, end = androidx.compose.ui.geometry.Offset(size.width * progress, 0f), strokeWidth = size.height)
    }
}
