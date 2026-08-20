package dev.vicent.veil.ui.components

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.ui.theme.LocalVeilPalette
import dev.vicent.veil.ui.theme.VeilMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private const val MEDIA_PROGRESS_TICK_MILLIS = 1_000L

@Composable
internal fun EmptyMediaTile(
    continuityEnabled: Boolean,
    onContinuityAccessRequested: () -> Unit,
    musicProviderLabel: String?,
    onOpenMusicProvider: (() -> Unit)?,
    onChooseMusicProvider: () -> Unit,
) {
    CozyTile(
        label = stringResource(R.string.media_tile_label),
        prominent = true,
        onClick = onOpenMusicProvider,
        modifier = Modifier.fillMaxWidth().heightIn(
            min = WorkspaceLayoutTokens.PRIMARY_TILE_HEIGHT,
        ),
    ) {
        EmptyMediaArtwork(musicProviderLabel = musicProviderLabel)
        if (onOpenMusicProvider != null && musicProviderLabel != null) {
            TileAction(
                stringResource(R.string.action_open_named, musicProviderLabel),
                onOpenMusicProvider,
            )
        } else {
            TileAction(stringResource(R.string.media_choose_provider), onChooseMusicProvider)
        }
        if (!continuityEnabled) {
            TileAction(stringResource(R.string.media_enable_continuity), onContinuityAccessRequested)
        }
    }
}

@Composable
private fun EmptyMediaArtwork(musicProviderLabel: String?) {
    val palette = LocalVeilPalette.current
    val description = if (musicProviderLabel != null) {
        stringResource(R.string.action_open_named, musicProviderLabel)
    } else {
        stringResource(R.string.media_no_playback)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = description
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(88.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(palette.fieldBackground.copy(alpha = .82f))
                    .border(1.dp, palette.divider, RoundedCornerShape(7.dp)),
            ) {
                ActivityGlyph(ActivityGlyphKind.MEDIA, size = 32.dp, isActive = false)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    Modifier
                        .fillMaxWidth(.78f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(palette.contentSecondary.copy(alpha = .22f)),
                )
                Box(
                    Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(.54f)
                        .height(11.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(palette.contentMuted.copy(alpha = .24f)),
                )
            }
        }
        SeekLine(progress = 0f, onSeek = {}, enabled = false, showPosition = false)
        BasicText(
            text = stringResource(R.string.media_unknown_timeline),
            style = workspaceMonoStyle(palette.contentMuted, 9),
            modifier = Modifier.padding(top = 5.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            listOf(
                stringResource(R.string.media_previous).uppercase(),
                stringResource(R.string.media_play).uppercase(),
                stringResource(R.string.media_next).uppercase(),
            ).forEach { label ->
                BasicText(
                    text = label,
                    style = workspaceMonoStyle(palette.contentMuted, 10),
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                )
            }
        }
    }
}

@Composable
internal fun MediaPlayerTile(
    media: ContinuityItem.Media,
    onAction: (String, ContinuityAction, Long?) -> Unit,
) {
    val palette = LocalVeilPalette.current
    var trackTransitionDirection by remember(media.id) { mutableIntStateOf(1) }
    val trackKey = remember(media.id, media.title, media.subtitle, media.durationMillis) {
        listOf(media.id, media.title, media.subtitle, media.durationMillis).joinToString("\u0000")
    }
    val track = remember(trackKey) {
        MediaTrackPresentation(
            key = trackKey,
            title = media.title,
            subtitle = media.subtitle,
            appLabel = media.appLabel,
            artwork = media.artwork?.asImageBitmap(),
        )
    }
    LaunchedEffect(track.key) {
        delay(VeilMotion.STANDARD_DURATION_MILLIS.toLong())
        trackTransitionDirection = 1
    }
    CozyTile(
        label = stringResource(R.string.media_now_playing),
        prominent = true,
        modifier = Modifier.fillMaxWidth().heightIn(
            min = WorkspaceLayoutTokens.PRIMARY_TILE_HEIGHT,
        ),
    ) {
        AnimatedContent(
            targetState = track,
            transitionSpec = {
                val direction = trackTransitionDirection
                (
                    fadeIn(
                        tween(
                            VeilMotion.STANDARD_DURATION_MILLIS,
                            easing = VeilMotion.enterEasing,
                        ),
                    ) + slideInHorizontally(
                        tween(
                            VeilMotion.STANDARD_DURATION_MILLIS,
                            easing = VeilMotion.standardEasing,
                        ),
                    ) { width -> width * direction / 5 }
                    ).togetherWith(
                    fadeOut(
                        tween(
                            VeilMotion.QUICK_DURATION_MILLIS,
                            easing = VeilMotion.exitEasing,
                        ),
                    ) + slideOutHorizontally(
                        tween(
                            VeilMotion.STANDARD_DURATION_MILLIS,
                            easing = VeilMotion.exitEasing,
                        ),
                    ) { width -> -width * direction / 7 },
                )
            },
            label = "media track change",
            modifier = Modifier.fillMaxWidth().height(88.dp),
        ) { displayedTrack ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                displayedTrack.artwork?.let { artwork ->
                    Image(
                        bitmap = artwork,
                        contentDescription = null,
                        modifier = Modifier.size(88.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    TileTitle(displayedTrack.title, prominent = true)
                    TileBody(
                        displayedTrack.subtitle
                            ?.let {
                                stringResource(R.string.media_artist_app, it, displayedTrack.appLabel)
                            }
                            ?: displayedTrack.appLabel,
                    )
                }
            }
        }
        val duration = media.durationMillis
        val position by produceState(
            initialValue = duration?.let { estimatedMediaPosition(media, it) } ?: 0L,
            media.id,
            media.positionMillis,
            media.positionUpdatedAtElapsedRealtime,
            media.isPlaying,
            media.playbackSpeed,
            duration,
        ) {
            if (duration != null) {
                do {
                    value = estimatedMediaPosition(media, duration)
                    if (media.isPlaying) delay(MEDIA_PROGRESS_TICK_MILLIS)
                } while (media.isPlaying && isActive)
            }
        }
        SeekLine(
            progress = if (duration != null) position.toFloat() / duration else 0f,
            onSeek = { ratio ->
                duration?.let {
                    onAction(media.id, ContinuityAction.SEEK_TO, (it * ratio).toLong())
                }
            },
            enabled = duration != null && ContinuityAction.SEEK_TO in media.supportedActions,
            showPosition = duration != null,
        )
        BasicText(
            text = if (duration != null) {
                stringResource(
                    R.string.media_timeline,
                    formatDuration(position),
                    formatDuration(duration),
                )
            } else {
                stringResource(R.string.media_unknown_timeline)
            },
            style = workspaceMonoStyle(palette.contentMuted, 9),
            modifier = Modifier.padding(top = 5.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            if (ContinuityAction.SKIP_PREVIOUS in media.supportedActions) {
                TileAction(stringResource(R.string.media_previous)) {
                    trackTransitionDirection = -1
                    onAction(media.id, ContinuityAction.SKIP_PREVIOUS, null)
                }
            }
            if (ContinuityAction.TOGGLE_PLAYBACK in media.supportedActions) {
                TileAction(
                    if (media.isPlaying) {
                        stringResource(R.string.media_pause)
                    } else {
                        stringResource(R.string.media_play)
                    },
                ) {
                    onAction(media.id, ContinuityAction.TOGGLE_PLAYBACK, null)
                }
            }
            if (ContinuityAction.SKIP_NEXT in media.supportedActions) {
                TileAction(stringResource(R.string.media_next)) {
                    trackTransitionDirection = 1
                    onAction(media.id, ContinuityAction.SKIP_NEXT, null)
                }
            }
        }
    }
}

private data class MediaTrackPresentation(
    val key: String,
    val title: String,
    val subtitle: String?,
    val appLabel: String,
    val artwork: ImageBitmap?,
)

private fun estimatedMediaPosition(media: ContinuityItem.Media, durationMillis: Long): Long {
    val basePosition = media.positionMillis ?: 0L
    val updatedAt = media.positionUpdatedAtElapsedRealtime
    val elapsed = if (media.isPlaying && updatedAt != null && media.playbackSpeed > 0f) {
        (SystemClock.elapsedRealtime() - updatedAt).coerceAtLeast(0L)
    } else {
        0L
    }
    return (basePosition + (elapsed * media.playbackSpeed).toLong())
        .coerceIn(0L, durationMillis)
}

@Composable
private fun SeekLine(
    progress: Float,
    onSeek: (Float) -> Unit,
    enabled: Boolean,
    showPosition: Boolean = true,
) {
    val palette = LocalVeilPalette.current
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .height(18.dp)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures { offset ->
                        onSeek((offset.x / size.width).coerceIn(0f, 1f))
                    }
                }
            },
    ) {
        val y = size.height / 2f
        drawLine(palette.divider, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 2.dp.toPx())
        if (showPosition) {
            drawLine(palette.accentActive, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width * progress, y), 2.dp.toPx())
            drawCircle(palette.contentPrimary, 3.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * progress, y))
        }
    }
}
