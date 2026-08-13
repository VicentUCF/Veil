package dev.vicent.veil.ui.components

import android.os.SystemClock
import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.vicent.veil.launcher.LauncherUiState
import dev.vicent.veil.launcher.ResolvedLauncherContext
import dev.vicent.veil.launcher.WorkspaceDataPolicy
import dev.vicent.veil.launcher.model.CalendarEventSummary
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.model.FocusTimerStatus
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.launcher.model.WeatherAvailability
import dev.vicent.veil.ui.theme.LocalVeilPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val PrimaryTileHeight = 220.dp
private val SecondaryTileHeight = 154.dp
private val SettingsTileHeight = 190.dp

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
    onSettingsSelected: (SettingsShortcut) -> Unit,
    onFocusStart: (Int) -> Unit,
    onFocusPause: () -> Unit,
    onFocusResume: () -> Unit,
    onFocusFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        // The outer 16 dp gutters are already consumed by LauncherScreen. A 328 dp
        // content width corresponds to the product's 360 dp screen breakpoint.
        val compact = maxWidth < 328.dp
        when (context.definition.kind) {
            LauncherContextKind.CURRENT -> CurrentWorkspace(
                state,
                compact,
                onCalendarPermissionRequested,
                onLocationPermissionRequested,
                onContinuityAccessRequested,
                onCalendarEventSelected,
                onContinuityAction,
            )
            LauncherContextKind.WORK -> WorkWorkspace(
                state,
                compact,
                context.quickActions.size,
                onCalendarPermissionRequested,
                onCalendarEventSelected,
                onContinuityAction,
                onFocusStart,
            )
            LauncherContextKind.MEDIA -> MediaWorkspace(
                state,
                compact,
                onContinuityAccessRequested,
                onContinuityAction,
                onSettingsSelected = { id -> settingsShortcuts.find { it.id == id }?.let(onSettingsSelected) },
            )
            LauncherContextKind.SOCIAL -> SocialWorkspace(compact)
            LauncherContextKind.TOOLS -> ToolsWorkspace(
                state,
                compact,
                onFocusStart,
                onFocusPause,
                onFocusResume,
                onFocusFinish,
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
                modifier = Modifier.fillMaxWidth().height(PrimaryTileHeight),
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
) {
    val workEvents = remember(state.calendarEvents) {
        WorkspaceDataPolicy.workEvents(state.calendarEvents, System.currentTimeMillis())
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CozyTile(
            label = "Agenda",
            prominent = true,
            modifier = Modifier.fillMaxWidth().height(PrimaryTileHeight),
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
            CozyTile(
                label = "Focus",
                onClick = { onFocusStart(25) },
                modifier = Modifier.fillMaxWidth().height(SecondaryTileHeight),
            ) {
                TileTitle("25:00")
                TileBody("Una entrada limpia a trabajo profundo.")
                TileAction("Empezar", onClick = { onFocusStart(25) })
            }
        })
    }
}

@Composable
private fun MediaWorkspace(
    state: LauncherUiState,
    compact: Boolean,
    onContinuityAccessRequested: () -> Unit,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
    onSettingsSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val media = state.mediaContinuity
        if (media != null) {
            MediaPlayerTile(media, onContinuityAction)
        } else {
            MediaLibraryTile(
                continuityEnabled = state.continuityAccessGranted,
                onContinuityAccessRequested = onContinuityAccessRequested,
            )
        }
        ResponsivePair(compact = compact, left = {
            CozyTile(
                label = "Volumen y salida",
                onClick = { onSettingsSelected("sound") },
                modifier = Modifier.fillMaxWidth().height(SecondaryTileHeight),
            ) {
                TileTitle("Control del sistema")
                TileBody("Abrir sonido y dispositivos de salida")
            }
        }, right = {
            CozyTile(
                label = if (media == null) "Colecciones" else "Biblioteca",
                modifier = Modifier.fillMaxWidth().height(SecondaryTileHeight),
            ) {
                TileTitle(if (media == null) "Todo listo" else "Después de esto")
                TileBody("Música · vídeo · directos · recuerdos")
                TileBody("Las fuentes están fijas en la barra inferior.")
            }
        })
    }
}

@Composable
private fun SocialWorkspace(compact: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CozyTile(
            label = "Directo",
            prominent = true,
            modifier = Modifier.fillMaxWidth().height(PrimaryTileHeight),
        ) {
            TileTitle("Tus canales, sin una bandeja más", prominent = true)
            TileBody("Elige abajo dónde quieres entrar. Las posiciones nunca cambian.")
            SocialModeRow("01", "Conversación", "texto y voz")
            SocialModeRow("02", "Comunidad", "grupos y servidores")
        }
        ResponsivePair(compact = compact, left = {
            CozyTile(
                label = "Comunidades",
                modifier = Modifier.fillMaxWidth().height(SecondaryTileHeight),
            ) {
                TileTitle("Entrar con intención")
                TileBody("Grupos, servidores y foros en un mismo contexto.")
            }
        }, right = {
            CozyTile(
                label = "Visual y llamadas",
                modifier = Modifier.fillMaxWidth().height(SecondaryTileHeight),
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
    onFocusStart: (Int) -> Unit,
    onFocusPause: () -> Unit,
    onFocusResume: () -> Unit,
    onFocusFinish: () -> Unit,
    onSettingsSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FocusTile(state, onFocusStart, onFocusPause, onFocusResume, onFocusFinish)
        ResponsivePair(compact = compact, left = {
            val system = state.systemStatus
            CozyTile(
                label = "Sistema",
                onClick = { onSettingsSelected("battery") },
                modifier = Modifier.fillMaxWidth().height(SecondaryTileHeight),
            ) {
                TileTitle("${system.batteryPercent}%${if (system.isCharging) " · cargando" else ""}")
                val freeGb = system.storageAvailableBytes / 1_073_741_824.0
                TileBody("${"%.1f".format(freeGb)} GB libres")
            }
        }, right = {
            CozyTile(
                label = "Conectividad",
                onClick = { onSettingsSelected("network") },
                modifier = Modifier.fillMaxWidth().height(SecondaryTileHeight),
            ) {
                TileTitle(state.systemStatus.connectionLabel)
                TileBody("Abrir redes y conexiones")
            }
        })
        SettingsPanel(onSettingsSelected)
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
        modifier = Modifier.fillMaxWidth().height(SecondaryTileHeight),
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
private fun MediaLibraryTile(
    continuityEnabled: Boolean,
    onContinuityAccessRequested: () -> Unit,
) {
    val palette = LocalVeilPalette.current
    CozyTile(
        label = "Biblioteca",
        prominent = true,
        modifier = Modifier.fillMaxWidth().height(PrimaryTileHeight),
    ) {
        TileTitle("Elige qué quieres escuchar o ver", prominent = true)
        BasicText(
            text = "▂  ▅  ▃  ▇  ▄  ▆  ▂  ▅  ▃  ▇",
            style = workspaceMonoStyle(palette.accentActive, 18),
            modifier = Modifier.padding(top = 18.dp),
        )
        TileBody("Música · vídeo · directos · recuerdos")
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
        modifier = Modifier.fillMaxWidth().height(SettingsTileHeight),
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
        modifier = Modifier.fillMaxWidth().height(PrimaryTileHeight),
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
    CozyTile(
        label = "Ahora suena",
        prominent = true,
        modifier = Modifier.fillMaxWidth().height(PrimaryTileHeight),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            media.artwork?.let { artwork ->
                Image(
                    bitmap = remember(artwork) { artwork.asImageBitmap() },
                    contentDescription = null,
                    modifier = Modifier.size(88.dp),
                )
                Spacer(Modifier.width(14.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                TileTitle(media.title, prominent = true)
                media.subtitle?.let { subtitle -> TileBody(subtitle) }
                TileBody(media.appLabel)
            }
        }
        val duration = media.durationMillis
        if (duration != null) {
            val basePosition = media.positionMillis ?: 0L
            val elapsed = if (media.isPlaying && media.positionUpdatedAtElapsedRealtime != null) {
                SystemClock.elapsedRealtime() - media.positionUpdatedAtElapsedRealtime
            } else 0L
            val position = (basePosition + elapsed).coerceIn(0L, duration)
            SeekLine(
                progress = position.toFloat() / duration,
                onSeek = { ratio -> onAction(media.id, ContinuityAction.SEEK_TO, (duration * ratio).toLong()) },
                enabled = ContinuityAction.SEEK_TO in media.supportedActions,
            )
            BasicText(
                text = "${formatDuration(position)}  /  ${formatDuration(duration)}",
                style = workspaceMonoStyle(palette.contentMuted, 9),
                modifier = Modifier.padding(top = 5.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            if (ContinuityAction.SKIP_PREVIOUS in media.supportedActions) {
                TileAction("Anterior") { onAction(media.id, ContinuityAction.SKIP_PREVIOUS, null) }
            }
            if (ContinuityAction.TOGGLE_PLAYBACK in media.supportedActions) {
                TileAction(if (media.isPlaying) "Pausa" else "Reproducir") {
                    onAction(media.id, ContinuityAction.TOGGLE_PLAYBACK, null)
                }
            }
            if (ContinuityAction.SKIP_NEXT in media.supportedActions) {
                TileAction("Siguiente") { onAction(media.id, ContinuityAction.SKIP_NEXT, null) }
            }
        }
    }
}

@Composable
private fun CalendarTile(
    events: List<CalendarEventSummary>,
    accessGranted: Boolean,
    onPermissionRequested: () -> Unit,
    onEventSelected: (Long) -> Unit,
) {
    CozyTile(label = "Próximo", modifier = Modifier.fillMaxWidth().height(SecondaryTileHeight)) {
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
    CozyTile(label = "Tiempo", modifier = Modifier.fillMaxWidth().height(SecondaryTileHeight)) {
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
private fun FocusTile(
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
        prominent = true,
        modifier = Modifier.fillMaxWidth().height(PrimaryTileHeight),
    ) {
        when (focus.status) {
            FocusTimerStatus.IDLE -> {
                TileTitle("Trabajo profundo", prominent = true)
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    TileAction("25 min") { onStart(25) }
                    TileAction("50 min") { onStart(50) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TileAction("−") { customMinutes = (customMinutes - 5).coerceAtLeast(5) }
                    BasicText(
                        text = "$customMinutes min",
                        style = workspaceMonoStyle(LocalVeilPalette.current.contentPrimary, 13),
                        modifier = Modifier.padding(horizontal = 14.dp),
                    )
                    TileAction("+") { customMinutes = (customMinutes + 5).coerceAtMost(180) }
                    TileAction("Iniciar") { onStart(customMinutes) }
                }
            }
            FocusTimerStatus.RUNNING, FocusTimerStatus.PAUSED -> {
                TileTitle(formatDuration(focus.remainingMillis), prominent = true)
                TileBody(if (focus.status == FocusTimerStatus.RUNNING) "Sesión en curso" else "Sesión pausada")
                Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
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
        if (!focus.exactAlarmAvailable || !focus.notificationsAvailable) {
            TileBody("El aviso fuera de Veil no está garantizado.")
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
private fun SeekLine(progress: Float, onSeek: (Float) -> Unit, enabled: Boolean) {
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
        drawLine(palette.accentActive, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width * progress, y), 2.dp.toPx())
        drawCircle(palette.contentPrimary, 3.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * progress, y))
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
