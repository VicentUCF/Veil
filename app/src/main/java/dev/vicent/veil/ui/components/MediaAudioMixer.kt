package dev.vicent.veil.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import dev.vicent.veil.launcher.model.AudioChannel
import dev.vicent.veil.launcher.model.AudioChannelLevel
import dev.vicent.veil.launcher.model.AudioMixerState
import dev.vicent.veil.launcher.model.AudioSpectrumAvailability
import dev.vicent.veil.ui.theme.LocalVeilPalette
import kotlin.math.roundToInt

private val AudioMixerTileHeight = 154.dp

@Composable
internal fun AudioMixerTile(
    state: AudioMixerState,
    compact: Boolean,
    isPlaying: Boolean,
    onVisualizerPermissionRequested: () -> Unit,
    onVolumeChanged: (AudioChannel, Float) -> Unit,
    onOpenSoundSettings: () -> Unit,
) {
    CozyTile(
        label = "Mezclador",
        modifier = Modifier.fillMaxWidth().heightIn(
            min = if (compact) 268.dp else AudioMixerTileHeight,
        ),
    ) {
        if (compact) {
            AudioVolumeControls(state.channels, onVolumeChanged)
            Spacer(Modifier.height(12.dp))
            AudioSpectrumPanel(
                state = state,
                isPlaying = isPlaying,
                onPermissionRequested = onVisualizerPermissionRequested,
                onOpenSoundSettings = onOpenSoundSettings,
            )
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AudioVolumeControls(
                    channels = state.channels,
                    onVolumeChanged = onVolumeChanged,
                    modifier = Modifier.weight(.45f),
                )
                AudioSpectrumPanel(
                    state = state,
                    isPlaying = isPlaying,
                    onPermissionRequested = onVisualizerPermissionRequested,
                    onOpenSoundSettings = onOpenSoundSettings,
                    modifier = Modifier.weight(.55f),
                )
            }
        }
    }
}

@Composable
private fun AudioVolumeControls(
    channels: List<AudioChannelLevel>,
    onVolumeChanged: (AudioChannel, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = modifier) {
        AudioChannel.entries.forEach { channel ->
            val level = channels.firstOrNull { it.channel == channel }
                ?: AudioChannelLevel(channel, 0, 1)
            AudioVolumeSlider(
                level = level,
                onChanged = { fraction -> onVolumeChanged(channel, fraction) },
            )
        }
    }
}

@Composable
private fun AudioVolumeSlider(
    level: AudioChannelLevel,
    onChanged: (Float) -> Unit,
) {
    val palette = LocalVeilPalette.current
    val label = when (level.channel) {
        AudioChannel.MEDIA -> "MULTIMEDIA"
        AudioChannel.RING -> "TONO"
        AudioChannel.ALARM -> "ALARMA"
    }
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(text = label, style = workspaceMonoStyle(palette.contentSecondary, 8))
            BasicText(
                text = "${(level.fraction * 100).roundToInt()}%",
                style = workspaceMonoStyle(palette.contentMuted, 8),
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(level.fraction, 0f..1f)
                    setProgress { target ->
                        onChanged(target.coerceIn(0f, 1f))
                        true
                    }
                }
                .pointerInput(level.channel) {
                    detectTapGestures { position ->
                        onChanged((position.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(level.channel) {
                    detectHorizontalDragGestures(
                        onDragStart = { position ->
                            onChanged((position.x / size.width).coerceIn(0f, 1f))
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            onChanged((change.position.x / size.width).coerceIn(0f, 1f))
                        },
                    )
                },
        ) {
            val y = size.height / 2f
            val progressX = size.width * level.fraction
            drawLine(palette.divider, Offset(0f, y), Offset(size.width, y), 3.dp.toPx())
            drawLine(palette.accentActive, Offset(0f, y), Offset(progressX, y), 3.dp.toPx())
            drawCircle(palette.contentPrimary, 4.dp.toPx(), Offset(progressX, y))
        }
    }
}

@Composable
private fun AudioSpectrumPanel(
    state: AudioMixerState,
    isPlaying: Boolean,
    onPermissionRequested: () -> Unit,
    onOpenSoundSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    Column(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(text = "ESPECTRO", style = workspaceMonoStyle(palette.contentSecondary, 8))
            BasicText(
                text = if (state.spectrumAvailability == AudioSpectrumAvailability.ACTIVE) "FFT · 50–10K" else "SALIDA",
                style = workspaceMonoStyle(palette.contentMuted, 8),
            )
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(76.dp).padding(top = 8.dp)) {
            val values = state.spectrum.ifEmpty { List(16) { 0f } }
            val gap = 2.dp.toPx()
            val barWidth = ((size.width - gap * (values.size - 1)) / values.size).coerceAtLeast(1f)
            values.forEachIndexed { index, value ->
                val normalized = value.coerceIn(0f, 1f)
                val barHeight = if (normalized > 0f) {
                    (size.height * normalized).coerceAtLeast(2.dp.toPx())
                } else {
                    1.dp.toPx()
                }
                drawRoundRect(
                    color = if (normalized > 0f) {
                        palette.accentActive.copy(alpha = .48f + normalized * .52f)
                    } else {
                        palette.divider
                    },
                    topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText("GRAVES", style = workspaceMonoStyle(palette.contentMuted, 7))
            BasicText("MEDIOS", style = workspaceMonoStyle(palette.contentMuted, 7))
            BasicText("AGUDOS", style = workspaceMonoStyle(palette.contentMuted, 7))
        }
        when (state.spectrumAvailability) {
            AudioSpectrumAvailability.NEEDS_PERMISSION ->
                TileAction("Activar espectro", onPermissionRequested)
            AudioSpectrumAvailability.UNAVAILABLE ->
                TileAction("Abrir sonido", onOpenSoundSettings)
            AudioSpectrumAvailability.IDLE -> BasicText(
                text = if (isPlaying) "Preparando señal…" else "En espera de reproducción",
                style = workspaceMonoStyle(palette.contentMuted, 8),
                modifier = Modifier.padding(top = 8.dp),
            )
            AudioSpectrumAvailability.ACTIVE -> Unit
        }
    }
}
