package dev.vicent.veil.ui.components

import android.os.SystemClock
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.vicent.veil.launcher.LauncherUiState
import dev.vicent.veil.launcher.ResolvedLauncherContext
import dev.vicent.veil.launcher.WorkspaceDataPolicy
import dev.vicent.veil.launcher.model.CalendarEventSummary
import dev.vicent.veil.launcher.model.AudioChannel
import dev.vicent.veil.launcher.model.AudioChannelLevel
import dev.vicent.veil.launcher.model.AudioMixerState
import dev.vicent.veil.launcher.model.AudioSpectrumAvailability
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.model.FocusTimerStatus
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.launcher.model.WeatherAvailability
import dev.vicent.veil.ui.theme.LocalVeilPalette
import dev.vicent.veil.ui.theme.VeilMotion
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val PrimaryTileHeight = 220.dp
private val SecondaryTileHeight = 154.dp
private val AudioMixerTileHeight = 154.dp
private val DeviceDashboardTileHeight = 184.dp
private val ToolsSecondaryTileHeight = 116.dp
private val SettingsTileHeight = 150.dp

@Composable
fun WorkspaceDashboard(
    state: LauncherUiState,
    context: ResolvedLauncherContext,
    settingsShortcuts: List<SettingsShortcut>,
    onCalendarPermissionRequested: () -> Unit,
    onLocationPermissionRequested: () -> Unit,
    onContinuityAccessRequested: () -> Unit,
    onCalendarEventSelected: (Long) -> Unit,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
    onHomeMediaDismissed: (String) -> Unit,
    onAudioVisualizerPermissionRequested: () -> Unit,
    onAudioVolumeChanged: (AudioChannel, Float) -> Unit,
    onSettingsSelected: (SettingsShortcut) -> Unit,
    onFocusStart: (Int) -> Unit,
    onFocusPause: () -> Unit,
    onFocusResume: () -> Unit,
    onFocusFinish: () -> Unit,
    onAppSelected: (dev.vicent.veil.launcher.model.LauncherApp) -> Unit,
    onAppLongPressed: (dev.vicent.veil.launcher.model.LauncherApp) -> Unit,
    onHomeButtonTap: () -> Unit,
    onHomeButtonLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        // The outer 16 dp gutters are already consumed by LauncherScreen. A 328 dp
        // content width corresponds to the product's 360 dp screen breakpoint.
        val compact = maxWidth < 328.dp
        when (context.definition.kind) {
            LauncherContextKind.CURRENT -> CurrentHome(
                state = state,
                apps = context.apps,
                onAppSelected = onAppSelected,
                onAppLongPressed = onAppLongPressed,
                onLocationPermissionRequested = onLocationPermissionRequested,
                onContinuityAction = onContinuityAction,
                onMediaDismissed = onHomeMediaDismissed,
                onQuickButtonTap = onHomeButtonTap,
                onQuickButtonLongPress = onHomeButtonLongPress,
                modifier = Modifier.fillMaxSize(),
            )
            LauncherContextKind.WORK -> WorkWorkspace(
                state,
                compact,
                context.quickActions.size,
                onCalendarPermissionRequested,
                onCalendarEventSelected,
                onContinuityAction,
                onFocusStart,
                onFocusPause,
                onFocusResume,
                onFocusFinish,
            )
            LauncherContextKind.MEDIA -> MediaWorkspace(
                state,
                compact,
                onContinuityAccessRequested,
                onContinuityAction,
                onAudioVisualizerPermissionRequested,
                onAudioVolumeChanged,
                onSettingsSelected = { id -> settingsShortcuts.find { it.id == id }?.let(onSettingsSelected) },
            )
            LauncherContextKind.SOCIAL -> SocialWorkspace(compact)
            LauncherContextKind.TOOLS -> ToolsWorkspace(
                state,
                compact,
                onSettingsSelected = { id -> settingsShortcuts.find { it.id == id }?.let(onSettingsSelected) },
            )
        }
    }
}

@Composable
private fun CurrentWorkspace(
    state: LauncherUiState,
    compact: Boolean,
    onCalendarPermissionRequested: () -> Unit,
    onLocationPermissionRequested: () -> Unit,
    onContinuityAccessRequested: () -> Unit,
    onCalendarEventSelected: (Long) -> Unit,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val continuity = state.currentContinuity
        if (continuity != null) {
            ContinuityTile(
                item = continuity,
                prominent = true,
                onAction = onContinuityAction,
                modifier = Modifier.fillMaxWidth().heightIn(min = PrimaryTileHeight),
            )
        } else {
            CalmCurrentTile(
                continuityEnabled = state.continuityAccessGranted,
                onContinuityAccessRequested = onContinuityAccessRequested,
            )
        }
        ResponsivePair(
            compact = compact,
            left = { CalendarTile(
                events = state.calendarEvents.take(1),
                accessGranted = state.calendarAccessGranted,
                onPermissionRequested = onCalendarPermissionRequested,
                onEventSelected = onCalendarEventSelected,
            ) },
            right = { WeatherTile(state, onLocationPermissionRequested) },
        )
    }
}

@Composable
private fun WorkWorkspace(
    state: LauncherUiState,
    compact: Boolean,
    pinnedToolCount: Int,
    onCalendarPermissionRequested: () -> Unit,
    onCalendarEventSelected: (Long) -> Unit,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
    onFocusStart: (Int) -> Unit,
    onFocusPause: () -> Unit,
    onFocusResume: () -> Unit,
    onFocusFinish: () -> Unit,
) {
    val workEvents = remember(state.calendarEvents) {
        WorkspaceDataPolicy.workEvents(state.calendarEvents, System.currentTimeMillis())
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CozyTile(
            label = "Agenda",
            prominent = true,
            modifier = Modifier.fillMaxWidth().heightIn(min = PrimaryTileHeight),
        ) {
            if (!state.calendarAccessGranted) {
                TileAction("Conectar calendario", onCalendarPermissionRequested)
            } else if (workEvents.isEmpty()) {
                TileTitle("Sin compromisos próximos")
                TileBody("El espacio queda libre para trabajar.")
            } else {
                workEvents.forEach { event -> EventRow(event, onCalendarEventSelected) }
            }
        }
        ResponsivePair(compact = compact, left = {
            WorkTerminalTile(
                agendaCount = workEvents.size,
                pinnedToolCount = pinnedToolCount,
                progress = state.workProgress,
                onContinuityAction = onContinuityAction,
            )
        }, right = {
            WorkFocusTile(
                state = state,
                onStart = onFocusStart,
                onPause = onFocusPause,
                onResume = onFocusResume,
                onFinish = onFocusFinish,
            )
        })
    }
}

@Composable
private fun MediaWorkspace(
    state: LauncherUiState,
    compact: Boolean,
    onContinuityAccessRequested: () -> Unit,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
    onAudioVisualizerPermissionRequested: () -> Unit,
    onAudioVolumeChanged: (AudioChannel, Float) -> Unit,
    onSettingsSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val media = state.mediaContinuity
        if (media != null) {
            MediaPlayerTile(media, onContinuityAction)
        } else {
            EmptyMediaTile(
                continuityEnabled = state.continuityAccessGranted,
                onContinuityAccessRequested = onContinuityAccessRequested,
            )
        }
        AudioMixerTile(
            state = state.audioMixer,
            compact = compact,
            isPlaying = media?.isPlaying == true,
            onVisualizerPermissionRequested = onAudioVisualizerPermissionRequested,
            onVolumeChanged = onAudioVolumeChanged,
            onOpenSoundSettings = { onSettingsSelected("sound") },
        )
    }
}

@Composable
private fun AudioMixerTile(
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

@Composable
private fun SocialWorkspace(compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CozyTile(
            label = "Directo",
            prominent = true,
            modifier = Modifier.fillMaxWidth().heightIn(min = PrimaryTileHeight),
        ) {
            TileTitle("Tus canales, sin una bandeja más", prominent = true)
            TileBody("Elige abajo dónde quieres entrar. Las posiciones nunca cambian.")
            SocialModeRow("01", "Conversación", "texto y voz")
            SocialModeRow("02", "Comunidad", "grupos y servidores")
        }
        ResponsivePair(compact = compact, left = {
            CozyTile(
                label = "Comunidades",
                modifier = Modifier.fillMaxWidth().heightIn(min = SecondaryTileHeight),
            ) {
                TileTitle("Entrar con intención")
                TileBody("Grupos, servidores y foros en un mismo contexto.")
            }
        }, right = {
            CozyTile(
                label = "Visual y llamadas",
                modifier = Modifier.fillMaxWidth().heightIn(min = SecondaryTileHeight),
            ) {
                TileTitle("Ver o estar")
                TileBody("Contenido visual, llamadas y presencia.")
            }
        })
    }
}

@Composable
private fun ToolsWorkspace(
    state: LauncherUiState,
    compact: Boolean,
    onSettingsSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DeviceDashboardTile(state, onSettingsSelected)
        ResponsivePair(compact = compact, left = {
            val system = state.systemStatus
            CozyTile(
                label = "Batería",
                modifier = Modifier.fillMaxWidth().heightIn(min = ToolsSecondaryTileHeight),
            ) {
                TileTitle(
                    system.batteryPercent
                        ?.let { "$it%${if (system.isCharging) " · cargando" else ""}" }
                        ?: "No disponible",
                )
                TileBody(
                    when {
                        system.batteryPercent == null -> "Android no ha publicado el estado"
                        system.isCharging -> "Conectado a la corriente"
                        else -> "Funcionando con batería"
                    },
                )
                TileAction("Abrir batería") { onSettingsSelected("battery") }
            }
        }, right = {
            CozyTile(
                label = "Conectividad",
                modifier = Modifier.fillMaxWidth().heightIn(min = ToolsSecondaryTileHeight),
            ) {
                TileTitle(state.systemStatus.connectionLabel)
                TileBody(
                    if (state.systemStatus.connectionLabel == "Sin conexión") {
                        "No hay una red activa"
                    } else {
                        "Transporte activo"
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    TileAction("Redes") { onSettingsSelected("network") }
                    TileAction("Bluetooth") { onSettingsSelected("bluetooth") }
                }
            }
        })
        SettingsPanel(onSettingsSelected)
    }
}

@Composable
private fun DeviceDashboardTile(
    state: LauncherUiState,
    onSettingsSelected: (String) -> Unit,
) {
    val system = state.systemStatus
    val deviceName = listOfNotNull(system.deviceManufacturer, system.deviceModel)
        .distinct()
        .joinToString(" ")
        .ifBlank { "Dispositivo no disponible" }
    val androidLabel = system.androidVersion?.let { "Android $it" } ?: "Android no disponible"
    val patchLabel = system.securityPatch?.let { "Parche $it" } ?: "Parche no disponible"

    CozyTile(
        label = "Dispositivo",
        prominent = true,
        modifier = Modifier.fillMaxWidth().heightIn(min = DeviceDashboardTileHeight),
    ) {
        TileTitle(deviceName, prominent = true)
        TileBody("$androidLabel · $patchLabel")
        DeviceMetric(
            label = "Almacenamiento",
            availableBytes = system.storageAvailableBytes,
            totalBytes = system.storageTotalBytes,
        )
        DeviceMetric(
            label = "Memoria RAM",
            availableBytes = system.memoryAvailableBytes,
            totalBytes = system.memoryTotalBytes,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            TileAction("Detalles") { onSettingsSelected("device_info") }
            TileAction("Almacenamiento") { onSettingsSelected("storage") }
        }
    }
}

@Composable
private fun DeviceMetric(label: String, availableBytes: Long, totalBytes: Long) {
    val palette = LocalVeilPalette.current
    val usedFraction = WorkspaceDataPolicy.usedFraction(availableBytes, totalBytes)
    val detail = if (usedFraction == null) {
        "NO DISPONIBLE"
    } else {
        val usedBytes = totalBytes - availableBytes.coerceIn(0L, totalBytes)
        "${formatCapacity(usedBytes)} / ${formatCapacity(totalBytes)}"
    }
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    ) {
        BasicText(label.uppercase(), style = workspaceMonoStyle(palette.contentMuted, 9))
        BasicText(detail, style = workspaceMonoStyle(palette.contentSecondary, 9))
    }
    if (usedFraction != null) {
        SimpleProgress(usedFraction)
    }
}

@Composable
private fun WorkTerminalTile(
    agendaCount: Int,
    pinnedToolCount: Int,
    progress: ContinuityItem.Progress?,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
) {
    val palette = LocalVeilPalette.current
    CozyTile(
        label = "Terminal",
        modifier = Modifier.fillMaxWidth().heightIn(min = SecondaryTileHeight),
    ) {
        BasicText(
            text = "veil@work:~ $ status",
            style = workspaceMonoStyle(palette.accentActive, 10),
        )
        BasicText(
            text = "agenda  ${if (agendaCount == 0) "clear" else "$agendaCount next"}",
            style = workspaceMonoStyle(palette.contentSecondary, 10),
            modifier = Modifier.padding(top = 8.dp),
        )
        if (progress == null) {
            BasicText(
                text = "session ready\n$pinnedToolCount tools pinned",
                style = workspaceMonoStyle(palette.contentMuted, 10),
                modifier = Modifier.padding(top = 5.dp),
            )
        } else {
            BasicText(
                text = progress.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = workspaceMonoStyle(palette.contentPrimary, 10),
                modifier = Modifier.padding(top = 5.dp),
            )
            progress.progress?.let { value -> SimpleProgress(value) }
            if (ContinuityAction.OPEN in progress.supportedActions) {
                TileAction("Retomar") {
                    onContinuityAction(progress.id, ContinuityAction.OPEN, null)
                }
            }
        }
    }
}

@Composable
private fun EmptyMediaTile(
    continuityEnabled: Boolean,
    onContinuityAccessRequested: () -> Unit,
) {
    val palette = LocalVeilPalette.current
    CozyTile(
        label = "Media",
        prominent = true,
        modifier = Modifier.fillMaxWidth().heightIn(min = PrimaryTileHeight),
    ) {
        TileTitle("Nada reproduciéndose", prominent = true)
        BasicText(
            text = "▂  ▅  ▃  ▇  ▄  ▆  ▂  ▅  ▃  ▇",
            style = workspaceMonoStyle(palette.accentActive, 18),
            modifier = Modifier.padding(top = 18.dp),
        )
        TileBody("Elige una fuente en la barra inferior para empezar.")
        if (!continuityEnabled) {
            TileAction("Activar continuidad", onContinuityAccessRequested)
        } else {
            TileBody("Cuando una sesión esté activa, esta superficie se convertirá en el reproductor.")
        }
    }
}

@Composable
private fun SocialModeRow(index: String, title: String, detail: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        BasicText(
            text = index,
            style = workspaceMonoStyle(LocalVeilPalette.current.accentActive, 9),
            modifier = Modifier.width(30.dp),
        )
        Column {
            BasicText(title, style = workspaceBodyStyle(LocalVeilPalette.current.contentPrimary))
            BasicText(detail, style = workspaceMonoStyle(LocalVeilPalette.current.contentMuted, 9))
        }
    }
}

@Composable
private fun SettingsPanel(onSettingsSelected: (String) -> Unit) {
    CozyTile(
        label = "Centro de control",
        modifier = Modifier.fillMaxWidth().heightIn(min = SettingsTileHeight),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsCell("Pantalla", "display", onSettingsSelected, Modifier.weight(1f))
            SettingsCell("Sonido", "sound", onSettingsSelected, Modifier.weight(1f))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            SettingsCell("Aplicaciones", "applications", onSettingsSelected, Modifier.weight(1f))
            SettingsCell("Seguridad", "security", onSettingsSelected, Modifier.weight(1f))
        }
        TileAction("Todos los ajustes") { onSettingsSelected("settings") }
    }
}

@Composable
private fun SettingsCell(
    label: String,
    id: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicText(
        text = label.uppercase(),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = workspaceMonoStyle(LocalVeilPalette.current.contentPrimary, 9),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(role = Role.Button, onClickLabel = "Abrir $label") { onSelected(id) }
            .padding(horizontal = 10.dp, vertical = 12.dp),
    )
}

@Composable
private fun ResponsivePair(
    compact: Boolean,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            left()
            right()
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f)) { left() }
            Column(modifier = Modifier.weight(1f)) { right() }
        }
    }
}

@Composable
private fun CalmCurrentTile(
    continuityEnabled: Boolean,
    onContinuityAccessRequested: () -> Unit,
) {
    val context = LocalContext.current
    val palette = LocalVeilPalette.current
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    val now = remember { Date() }
    CozyTile(
        label = "Ahora",
        prominent = true,
        modifier = Modifier.fillMaxWidth().heightIn(min = PrimaryTileHeight),
    ) {
        BasicText(
            text = DateFormat.getTimeFormat(context).format(now),
            style = workspaceMonoStyle(palette.contentPrimary, 34),
        )
        BasicText(
            text = SimpleDateFormat("EEEE, d MMMM", locale).format(now),
            style = workspaceBodyStyle(palette.contentSecondary),
            modifier = Modifier.padding(top = 7.dp),
        )
        TileBody("Todo está tranquilo. Veil espera una actividad que continuar.")
        if (!continuityEnabled) TileAction("Activar continuidad", onContinuityAccessRequested)
    }
}

@Composable
private fun ContinuityTile(
    item: ContinuityItem,
    prominent: Boolean,
    onAction: (String, ContinuityAction, Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    CozyTile(label = continuityLabel(item), prominent = prominent, modifier = modifier) {
        TileTitle(item.title, prominent)
        item.subtitle?.let { subtitle -> TileBody(subtitle) }
        TileBody(item.appLabel)
        if (item is ContinuityItem.Progress && item.progress != null) {
            SimpleProgress(item.progress)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            if (ContinuityAction.TOGGLE_PLAYBACK in item.supportedActions && item is ContinuityItem.Media) {
                TileAction(if (item.isPlaying) "Pausa" else "Reproducir") {
                    onAction(item.id, ContinuityAction.TOGGLE_PLAYBACK, null)
                }
            }
            if (ContinuityAction.OPEN in item.supportedActions) {
                TileAction("Retomar") { onAction(item.id, ContinuityAction.OPEN, null) }
            }
        }
    }
}

@Composable
private fun MediaPlayerTile(
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
        delay(VeilMotion.StandardDurationMillis.toLong())
        trackTransitionDirection = 1
    }
    CozyTile(
        label = "Ahora suena",
        prominent = true,
        modifier = Modifier.fillMaxWidth().heightIn(min = PrimaryTileHeight),
    ) {
        AnimatedContent(
            targetState = track,
            transitionSpec = {
                val direction = trackTransitionDirection
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
                    ) { width -> width * direction / 5 }
                    ).togetherWith(
                    fadeOut(
                        tween(
                            VeilMotion.QuickDurationMillis,
                            easing = VeilMotion.exitEasing,
                        ),
                    ) + slideOutHorizontally(
                        tween(
                            VeilMotion.StandardDurationMillis,
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
                            ?.let { "$it · ${displayedTrack.appLabel}" }
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
                "${formatDuration(position)}  /  ${formatDuration(duration)}"
            } else {
                "--:--  /  --:--"
            },
            style = workspaceMonoStyle(palette.contentMuted, 9),
            modifier = Modifier.padding(top = 5.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            if (ContinuityAction.SKIP_PREVIOUS in media.supportedActions) {
                TileAction("Anterior") {
                    trackTransitionDirection = -1
                    onAction(media.id, ContinuityAction.SKIP_PREVIOUS, null)
                }
            }
            if (ContinuityAction.TOGGLE_PLAYBACK in media.supportedActions) {
                TileAction(if (media.isPlaying) "Pausa" else "Reproducir") {
                    onAction(media.id, ContinuityAction.TOGGLE_PLAYBACK, null)
                }
            }
            if (ContinuityAction.SKIP_NEXT in media.supportedActions) {
                TileAction("Siguiente") {
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

private const val MEDIA_PROGRESS_TICK_MILLIS = 1_000L

@Composable
private fun CalendarTile(
    events: List<CalendarEventSummary>,
    accessGranted: Boolean,
    onPermissionRequested: () -> Unit,
    onEventSelected: (Long) -> Unit,
) {
    CozyTile(
        label = "Próximo",
        modifier = Modifier.fillMaxWidth().heightIn(min = SecondaryTileHeight),
    ) {
        when {
            !accessGranted -> TileAction("Conectar calendario", onPermissionRequested)
            events.isEmpty() -> {
                TileTitle("Agenda libre")
                TileBody("No hay eventos próximos.")
            }
            else -> events.forEach { EventRow(it, onEventSelected) }
        }
    }
}

@Composable
private fun WeatherTile(state: LauncherUiState, onPermissionRequested: () -> Unit) {
    val weather = state.weather
    val uriHandler = LocalUriHandler.current
    CozyTile(
        label = "Tiempo",
        modifier = Modifier.fillMaxWidth().heightIn(min = SecondaryTileHeight),
    ) {
        when (weather.availability) {
            WeatherAvailability.NEEDS_PERMISSION -> TileAction("Usar ubicación aproximada", onPermissionRequested)
            WeatherAvailability.LOADING -> TileBody("Actualizando…")
            WeatherAvailability.UNAVAILABLE -> TileBody("Tiempo no disponible")
            WeatherAvailability.AVAILABLE -> {
                TileTitle("${weather.temperatureCelsius?.roundToInt() ?: "—"}° · ${weatherDescription(weather.weatherCode)}")
                TileBody(
                    "Sensación ${weather.apparentTemperatureCelsius?.roundToInt() ?: "—"}°  ·  " +
                        "${weather.minimumCelsius?.roundToInt() ?: "—"}° / ${weather.maximumCelsius?.roundToInt() ?: "—"}°" +
                        if (weather.isStale) " · desactualizado" else "",
                )
                TileAction("Open‑Meteo") { uriHandler.openUri("https://open-meteo.com/") }
            }
        }
    }
}

@Composable
private fun WorkFocusTile(
    state: LauncherUiState,
    onStart: (Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
) {
    val focus = state.focusTimer
    var customMinutes by remember { mutableIntStateOf(25) }
    CozyTile(
        label = "Focus",
        modifier = Modifier.fillMaxWidth().heightIn(min = SecondaryTileHeight),
    ) {
        when (focus.status) {
            FocusTimerStatus.IDLE -> {
                TileTitle("Trabajo profundo")
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    TileAction("25 min") { onStart(25) }
                    TileAction("50 min") { onStart(50) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TileAction("−") { customMinutes = (customMinutes - 5).coerceAtLeast(5) }
                    BasicText(
                        text = "$customMinutes min",
                        style = workspaceMonoStyle(LocalVeilPalette.current.contentPrimary, 10),
                        modifier = Modifier.padding(horizontal = 7.dp),
                    )
                    TileAction("+") { customMinutes = (customMinutes + 5).coerceAtMost(180) }
                    TileAction("Ir") { onStart(customMinutes) }
                }
            }
            FocusTimerStatus.RUNNING, FocusTimerStatus.PAUSED -> {
                TileTitle(formatDuration(focus.remainingMillis))
                val status = if (focus.status == FocusTimerStatus.RUNNING) "Sesión en curso" else "Sesión pausada"
                val warning = if (!focus.exactAlarmAvailable || !focus.notificationsAvailable) " · aviso limitado" else ""
                TileBody(status + warning)
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    if (focus.status == FocusTimerStatus.RUNNING) TileAction("Pausa", onPause)
                    else TileAction("Reanudar", onResume)
                    TileAction("Finalizar", onFinish)
                }
            }
            FocusTimerStatus.COMPLETED -> {
                TileTitle("Sesión completada", prominent = true)
                TileAction("Cerrar", onFinish)
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEventSummary, onSelected: (Long) -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "Abrir ${event.title}") { onSelected(event.id) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = DateFormat.getTimeFormat(context).format(Date(event.startMillis)),
            style = workspaceMonoStyle(LocalVeilPalette.current.accentActive, 10),
            modifier = Modifier.width(58.dp),
        )
        BasicText(
            text = event.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = workspaceBodyStyle(LocalVeilPalette.current.contentPrimary),
        )
    }
}

@Composable private fun TileTitle(text: String, prominent: Boolean = false) = BasicText(
    text = text,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    style = workspaceTitleStyle(LocalVeilPalette.current.contentPrimary, prominent),
)

@Composable private fun TileBody(text: String) = BasicText(
    text = text,
    maxLines = 3,
    overflow = TextOverflow.Ellipsis,
    style = workspaceBodyStyle(LocalVeilPalette.current.contentSecondary),
    modifier = Modifier.padding(top = 5.dp),
)

@Composable private fun TileAction(label: String, onClick: () -> Unit) = BasicText(
    text = label.uppercase(),
    style = workspaceMonoStyle(LocalVeilPalette.current.contentPrimary, 10),
    modifier = Modifier
        .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
        .padding(vertical = 10.dp, horizontal = 2.dp),
)

@Composable
private fun SimpleProgress(progress: Float) {
    val palette = LocalVeilPalette.current
    Canvas(modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(2.dp)) {
        drawRect(palette.divider)
        drawRect(palette.accentActive, size = size.copy(width = size.width * progress.coerceIn(0f, 1f)))
    }
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
                if (enabled) detectTapGestures { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f)) }
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

private fun continuityLabel(item: ContinuityItem): String = when (item) {
    is ContinuityItem.Media -> if (item.isPlaying) "Ahora suena" else "Continuar"
    is ContinuityItem.Navigation -> "Navegación"
    is ContinuityItem.Progress -> if (item.isComplete) "Completado" else "En progreso"
}

private fun weatherDescription(code: Int?): String = when (code) {
    0 -> "Despejado"
    1, 2 -> "Parcialmente nublado"
    3 -> "Cubierto"
    45, 48 -> "Niebla"
    in 51..57 -> "Llovizna"
    in 61..67 -> "Lluvia"
    in 71..77 -> "Nieve"
    in 80..82 -> "Chubascos"
    in 95..99 -> "Tormenta"
    else -> "Variable"
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun formatCapacity(bytes: Long): String {
    if (bytes < 0L) return "No disponible"
    val gibibytes = bytes / 1_073_741_824.0
    return if (gibibytes >= 1.0) {
        String.format(Locale.getDefault(), "%.1f GB", gibibytes)
    } else {
        String.format(Locale.getDefault(), "%.0f MB", bytes / 1_048_576.0)
    }
}
