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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.vicent.veil.launcher.LauncherUiState
import dev.vicent.veil.launcher.QuickNotesPolicy
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
import dev.vicent.veil.launcher.model.QuickNote
import dev.vicent.veil.launcher.model.QuickNoteChecklistItem
import dev.vicent.veil.launcher.model.QuickNoteType
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.launcher.model.WeatherAvailability
import dev.vicent.veil.ui.theme.LocalVeilPalette
import dev.vicent.veil.ui.theme.VeilMotion
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.cos
import kotlin.math.sin
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
    onClockOpenRequested: () -> Unit,
    onContinuityAccessRequested: () -> Unit,
    onCalendarEventSelected: (Long) -> Unit,
    onCalendarEventCreateRequested: () -> Unit,
    onCalendarOpenRequested: () -> Unit,
    onGoogleCalendarConfigureRequested: () -> Unit,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
    onHomeMediaDismissed: (String) -> Unit,
    onAudioVisualizerPermissionRequested: () -> Unit,
    onAudioVolumeChanged: (AudioChannel, Float) -> Unit,
    onSettingsSelected: (SettingsShortcut) -> Unit,
    onVeilSettingsSelected: () -> Unit,
    onFocusStart: (Int) -> Unit,
    onFocusPause: () -> Unit,
    onFocusResume: () -> Unit,
    onFocusFinish: () -> Unit,
    onQuickNoteAdded: (String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onQuickNoteUpdated: (Long, String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onQuickNoteDeleted: (Long) -> Unit,
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
                onClockOpenRequested = onClockOpenRequested,
                onCalendarOpenRequested = onCalendarOpenRequested,
                onContinuityAction = onContinuityAction,
                onMediaDismissed = onHomeMediaDismissed,
                onQuickButtonTap = onHomeButtonTap,
                onQuickButtonLongPress = onHomeButtonLongPress,
                modifier = Modifier.fillMaxSize(),
            )
            LauncherContextKind.WORK -> WorkWorkspace(
                state,
                compact,
                onCalendarPermissionRequested,
                onCalendarEventSelected,
                onCalendarEventCreateRequested,
                onCalendarOpenRequested,
                onGoogleCalendarConfigureRequested,
                onContinuityAction,
                onFocusStart,
                onFocusPause,
                onFocusResume,
                onFocusFinish,
                onQuickNoteAdded,
                onQuickNoteUpdated,
                onQuickNoteDeleted,
            )
            LauncherContextKind.MEDIA -> MediaWorkspace(
                state,
                compact,
                onContinuityAccessRequested,
                onContinuityAction,
                onAudioVisualizerPermissionRequested,
                onAudioVolumeChanged,
                onSettingsSelected = { id -> settingsShortcuts.find { it.id == id }?.let(onSettingsSelected) },
                onAppSelected = onAppSelected,
            )
            LauncherContextKind.SOCIAL -> SocialWorkspace(compact)
            LauncherContextKind.TOOLS -> ToolsWorkspace(
                state,
                compact,
                onVeilSettingsSelected,
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
    onCalendarPermissionRequested: () -> Unit,
    onCalendarEventSelected: (Long) -> Unit,
    onCalendarEventCreateRequested: () -> Unit,
    onCalendarOpenRequested: () -> Unit,
    onGoogleCalendarConfigureRequested: () -> Unit,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
    onFocusStart: (Int) -> Unit,
    onFocusPause: () -> Unit,
    onFocusResume: () -> Unit,
    onFocusFinish: () -> Unit,
    onQuickNoteAdded: (String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onQuickNoteUpdated: (Long, String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onQuickNoteDeleted: (Long) -> Unit,
) {
    val workEvents = remember(state.calendarEvents) {
        WorkspaceDataPolicy.workEvents(state.calendarEvents, System.currentTimeMillis())
    }
    var agendaDialog by remember { mutableStateOf<AgendaDialogMode?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CozyTile(
            label = "Agenda",
            prominent = true,
            onClick = { agendaDialog = AgendaDialogMode.ACTIONS },
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
            state.workProgress?.let { progress ->
                WorkProgressSummary(progress, onContinuityAction)
            }
        }
        WorkSecondaryRow(compact = compact, notes = {
            WorkQuickNotesTile(
                notes = state.quickNotes,
                onAdd = onQuickNoteAdded,
                onUpdate = onQuickNoteUpdated,
                onDelete = onQuickNoteDeleted,
            )
        }, pomodoro = {
            WorkPomodoroTile(
                state = state,
                compact = compact,
                onStart = onFocusStart,
                onPause = onFocusPause,
                onResume = onFocusResume,
                onFinish = onFocusFinish,
            )
        })
    }

    agendaDialog?.let { mode ->
        AgendaRofiDialog(
            mode = mode,
            events = state.calendarEvents,
            accessGranted = state.calendarAccessGranted,
            onDismiss = { agendaDialog = null },
            onBack = { agendaDialog = AgendaDialogMode.ACTIONS },
            onShowWeek = { agendaDialog = AgendaDialogMode.WEEK },
            onPermissionRequested = {
                agendaDialog = null
                onCalendarPermissionRequested()
            },
            onCreateEvent = {
                agendaDialog = null
                onCalendarEventCreateRequested()
            },
            onOpenCalendar = {
                agendaDialog = null
                onCalendarOpenRequested()
            },
            onConfigureGoogle = {
                agendaDialog = null
                onGoogleCalendarConfigureRequested()
            },
            onEventSelected = { eventId ->
                agendaDialog = null
                onCalendarEventSelected(eventId)
            },
        )
    }
}

private enum class AgendaDialogMode { ACTIONS, WEEK }

@Composable
private fun AgendaRofiDialog(
    mode: AgendaDialogMode,
    events: List<CalendarEventSummary>,
    accessGranted: Boolean,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onShowWeek: () -> Unit,
    onPermissionRequested: () -> Unit,
    onCreateEvent: () -> Unit,
    onOpenCalendar: () -> Unit,
    onConfigureGoogle: () -> Unit,
    onEventSelected: (Long) -> Unit,
) {
    if (mode == AgendaDialogMode.WEEK) {
        WeekSummaryDialog(
            events = events,
            onDismiss = onDismiss,
            onBack = onBack,
            onEventSelected = onEventSelected,
        )
        return
    }
    RofiDialog(
        title = "agenda",
        onDismiss = onDismiss,
        actions = { RofiAction("cerrar", onDismiss) },
    ) {
        if (!accessGranted) {
            AgendaCommand(
                command = "conectar_calendario",
                detail = "Permitir que Veil lea los calendarios visibles de Android",
                onClick = onPermissionRequested,
            )
        } else {
            AgendaCommand(
                command = "resumen_semana",
                detail = "Ver los próximos siete días",
                onClick = onShowWeek,
            )
            AgendaCommand(
                command = "nuevo_evento",
                detail = "Abrir el compositor del calendario instalado",
                onClick = onCreateEvent,
            )
            AgendaCommand(
                command = "abrir_calendario",
                detail = "Continuar en tu aplicación de calendario",
                onClick = onOpenCalendar,
            )
        }
        AgendaCommand(
            command = "configurar_google",
            detail = "Abrir Google Calendar para cuenta y sincronización",
            onClick = onConfigureGoogle,
        )
        RofiBody(
            "Veil combina todos los calendarios visibles sincronizados por Android, " +
                "incluidos los eventos de Google Calendar.",
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun AgendaCommand(command: String, detail: String, onClick: () -> Unit) {
    val palette = LocalVeilPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(palette.fieldBackground.copy(alpha = 0.52f))
            .border(1.dp, palette.divider, RoundedCornerShape(3.dp))
            .clickable(role = Role.Button, onClickLabel = command, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        BasicText(">", style = workspaceMonoStyle(palette.accentActive, 11))
        Column(modifier = Modifier.padding(start = 9.dp)) {
            BasicText(command, style = workspaceMonoStyle(palette.contentPrimary, 10))
            BasicText(
                detail,
                style = workspaceMonoStyle(palette.contentMuted, 8),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun WeekSummaryDialog(
    events: List<CalendarEventSummary>,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onEventSelected: (Long) -> Unit,
) {
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    val dayKey = remember(locale) { SimpleDateFormat("yyyyMMdd", locale) }
    val dayLabel = remember(locale) { SimpleDateFormat("EEEE, d MMM", locale) }
    val groupedEvents = remember(events, locale) {
        events.groupBy { event -> dayKey.format(Date(event.startMillis)) }
    }
    RofiDialog(
        title = "resumen semana",
        onDismiss = onDismiss,
        actions = {
            RofiAction("volver", onBack)
            RofiAction("cerrar", onDismiss)
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            if (groupedEvents.isEmpty()) {
                RofiBody("No hay eventos visibles en los próximos siete días.")
            } else {
                groupedEvents.values.forEach { dayEvents ->
                    BasicText(
                        dayLabel.format(Date(dayEvents.first().startMillis)).uppercase(locale),
                        style = workspaceMonoStyle(LocalVeilPalette.current.accentActive, 9),
                    )
                    dayEvents.forEach { event -> WeekEventRow(event, onEventSelected) }
                }
            }
        }
    }
}

@Composable
private fun WeekEventRow(event: CalendarEventSummary, onSelected: (Long) -> Unit) {
    val context = LocalContext.current
    val palette = LocalVeilPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = "Abrir ${event.title}",
            ) { onSelected(event.id) }
            .padding(vertical = 7.dp),
    ) {
        BasicText(
            DateFormat.getTimeFormat(context).format(Date(event.startMillis)),
            style = workspaceMonoStyle(palette.accentActive, 10),
            modifier = Modifier.width(58.dp),
        )
        BasicText(
            event.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = workspaceMonoStyle(palette.contentPrimary, 10),
        )
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
    onAppSelected: (dev.vicent.veil.launcher.model.LauncherApp) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val media = state.mediaContinuity
        if (media != null) {
            MediaPlayerTile(media, onContinuityAction)
        } else {
            val youtubeMusic = YouTubeMusicPackages.firstNotNullOfOrNull { packageName ->
                state.installedApps.firstOrNull { app -> app.packageName == packageName }
            }
            EmptyMediaTile(
                continuityEnabled = state.continuityAccessGranted,
                onContinuityAccessRequested = onContinuityAccessRequested,
                onOpenYouTubeMusic = youtubeMusic?.let { app -> { onAppSelected(app) } },
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
    onVeilSettingsSelected: () -> Unit,
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
        SettingsPanel(onVeilSettingsSelected, onSettingsSelected)
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
private fun WorkProgressSummary(
    progress: ContinuityItem.Progress,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
) {
    val palette = LocalVeilPalette.current
    Spacer(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .height(1.dp)
            .background(palette.divider),
    )
    BasicText(
        text = if (progress.isComplete) "COMPLETADO" else "EN CURSO",
        style = workspaceMonoStyle(palette.accentActive, 9),
        modifier = Modifier.padding(top = 8.dp),
    )
    BasicText(
        text = progress.title,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = workspaceBodyStyle(palette.contentPrimary),
        modifier = Modifier.padding(top = 4.dp),
    )
    progress.progress?.let { value -> SimpleProgress(value) }
    if (ContinuityAction.OPEN in progress.supportedActions) {
        TileAction("Retomar") {
            onContinuityAction(progress.id, ContinuityAction.OPEN, null)
        }
    }
}

@Composable
private fun WorkQuickNotesTile(
    notes: List<QuickNote>,
    onAdd: (String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onUpdate: (Long, String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var editingNote by remember { mutableStateOf<QuickNote?>(null) }
    var creatingNote by remember { mutableStateOf(false) }
    CozyTile(
        label = "Notas rápidas",
        modifier = Modifier.fillMaxWidth().heightIn(min = SecondaryTileHeight),
    ) {
        if (notes.isEmpty()) {
            TileBody("Captura una idea sin salir de WORK.")
        } else {
            notes.forEach { note ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Editar ${note.title}",
                        ) { editingNote = note }
                        .padding(vertical = 5.dp),
                ) {
                    BasicText(
                        text = if (note.type == QuickNoteType.CHECKLIST) "[ ]" else "·",
                        style = workspaceMonoStyle(LocalVeilPalette.current.accentActive, 11),
                        modifier = Modifier.padding(end = 7.dp),
                    )
                    BasicText(
                        text = note.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = workspaceBodyStyle(LocalVeilPalette.current.contentPrimary),
                    )
                }
            }
        }
        if (notes.size < 3) TileAction("Añadir") { creatingNote = true }
    }

    if (creatingNote) {
        QuickNoteEditorDialog(
            note = null,
            onDismiss = { creatingNote = false },
            onSave = { title, type, body, checklist ->
                creatingNote = false
                onAdd(title, type, body, checklist)
            },
            onDelete = null,
        )
    }
    editingNote?.let { note ->
        QuickNoteEditorDialog(
            note = note,
            onDismiss = { editingNote = null },
            onSave = { title, type, body, checklist ->
                editingNote = null
                onUpdate(note.id, title, type, body, checklist)
            },
            onDelete = { editingNote = null; onDelete(note.id) },
        )
    }
}

@Composable
private fun QuickNoteEditorDialog(
    note: QuickNote?,
    onDismiss: () -> Unit,
    onSave: (String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onDelete: (() -> Unit)?,
) {
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var type by remember(note?.id) { mutableStateOf(note?.type ?: QuickNoteType.TEXT) }
    var body by remember(note?.id) { mutableStateOf(note?.body.orEmpty()) }
    var checklist by remember(note?.id) { mutableStateOf(note?.checklist.orEmpty()) }
    val validTitle = QuickNotesPolicy.sanitizeTitle(title)
    RofiDialog(
        title = if (note == null) "nueva nota" else "editar nota",
        onDismiss = onDismiss,
        actions = {
            if (onDelete != null) RofiAction("eliminar", onDelete, danger = true)
            Spacer(Modifier.weight(1f))
            RofiAction("cancelar", onDismiss)
            RofiAction(
                label = "guardar",
                enabled = validTitle != null,
                onClick = {
                    validTitle?.let { cleanTitle ->
                        onSave(cleanTitle, type, body, checklist)
                    }
                },
            )
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            RofiEditorField(
                label = "title",
                value = title,
                hint = "visible en WORK · ${title.length}/${QuickNotesPolicy.MaxTitleLength}",
                singleLine = true,
                onValueChange = { value ->
                    title = value.replace('\n', ' ').replace('\r', ' ')
                        .take(QuickNotesPolicy.MaxTitleLength)
                },
            )
            RofiNoteTypeSelector(selected = type, onSelected = { type = it })
            when (type) {
                QuickNoteType.TEXT -> RofiEditorField(
                    label = "content",
                    value = body,
                    hint = "texto libre · ${body.length}/${QuickNotesPolicy.MaxBodyLength}",
                    minHeight = 170.dp,
                    onValueChange = { body = it.take(QuickNotesPolicy.MaxBodyLength) },
                )
                QuickNoteType.CHECKLIST -> {
                    checklist.forEach { item ->
                        RofiChecklistEditorRow(
                            item = item,
                            onCheckedChange = { checked ->
                                checklist = checklist.map { current ->
                                    if (current.id == item.id) current.copy(checked = checked)
                                    else current
                                }
                            },
                            onTextChange = { value ->
                                checklist = checklist.map { current ->
                                    if (current.id == item.id) current.copy(
                                        text = value.replace('\n', ' ').replace('\r', ' ')
                                            .take(QuickNotesPolicy.MaxChecklistItemLength),
                                    ) else current
                                }
                            },
                            onDelete = {
                                checklist = checklist.filterNot { it.id == item.id }
                            },
                        )
                    }
                    if (checklist.size < QuickNotesPolicy.MaxChecklistItems) {
                        RofiAction(
                            label = "+ item",
                            onClick = {
                                val nextId =
                                    (checklist.maxOfOrNull(QuickNoteChecklistItem::id) ?: 0L) + 1L
                                checklist = checklist + QuickNoteChecklistItem(nextId, "")
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RofiEditorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    singleLine: Boolean = false,
    minHeight: Dp = 46.dp,
) {
    val palette = LocalVeilPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            BasicText("$label:", style = workspaceMonoStyle(palette.accentActive, 9))
            BasicText(hint, style = workspaceMonoStyle(palette.contentMuted, 8))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            textStyle = workspaceMonoStyle(palette.contentPrimary, 11),
            cursorBrush = SolidColor(palette.accentActive),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(RoundedCornerShape(3.dp))
                .background(palette.fieldBackground.copy(alpha = 0.72f))
                .border(1.dp, palette.divider, RoundedCornerShape(3.dp))
                .padding(horizontal = 11.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun RofiNoteTypeSelector(
    selected: QuickNoteType,
    onSelected: (QuickNoteType) -> Unit,
) {
    val palette = LocalVeilPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        BasicText("mode:", style = workspaceMonoStyle(palette.accentActive, 9))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            listOf(
                QuickNoteType.TEXT to "texto",
                QuickNoteType.CHECKLIST to "checklist",
            ).forEach { (type, label) ->
                val active = selected == type
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (active) palette.accentActive.copy(alpha = 0.15f)
                            else palette.fieldBackground.copy(alpha = 0.56f),
                        )
                        .border(
                            1.dp,
                            if (active) palette.accentActive else palette.divider,
                            RoundedCornerShape(3.dp),
                        )
                        .clickable(role = Role.RadioButton) { onSelected(type) }
                        .padding(horizontal = 11.dp, vertical = 10.dp),
                ) {
                    BasicText(
                        if (active) ">" else " ",
                        style = workspaceMonoStyle(palette.accentActive, 10),
                    )
                    BasicText(
                        label,
                        style = workspaceMonoStyle(
                            if (active) palette.contentPrimary else palette.contentSecondary,
                            10,
                        ),
                        modifier = Modifier.padding(start = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RofiChecklistEditorRow(
    item: QuickNoteChecklistItem,
    onCheckedChange: (Boolean) -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalVeilPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(42.dp)
                .clickable(role = Role.Checkbox) { onCheckedChange(!item.checked) },
        ) {
            BasicText(
                if (item.checked) "[x]" else "[ ]",
                style = workspaceMonoStyle(
                    if (item.checked) palette.accentActive else palette.contentSecondary,
                    11,
                ),
            )
        }
        BasicTextField(
            value = item.text,
            onValueChange = onTextChange,
            singleLine = true,
            textStyle = workspaceMonoStyle(palette.contentPrimary, 10),
            cursorBrush = SolidColor(palette.accentActive),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 42.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(palette.fieldBackground.copy(alpha = 0.72f))
                .border(1.dp, palette.divider, RoundedCornerShape(3.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp),
        )
        RofiAction("x", onDelete, danger = true)
    }
}

@Composable
private fun EmptyMediaTile(
    continuityEnabled: Boolean,
    onContinuityAccessRequested: () -> Unit,
    onOpenYouTubeMusic: (() -> Unit)?,
) {
    CozyTile(
        label = "Media",
        prominent = true,
        onClick = onOpenYouTubeMusic,
        modifier = Modifier.fillMaxWidth().heightIn(min = PrimaryTileHeight),
    ) {
        EmptyMediaArtwork(canOpenYouTubeMusic = onOpenYouTubeMusic != null)
        if (!continuityEnabled) {
            TileAction("Activar continuidad", onContinuityAccessRequested)
        }
    }
}

@Composable
private fun EmptyMediaArtwork(canOpenYouTubeMusic: Boolean) {
    val palette = LocalVeilPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = if (canOpenYouTubeMusic) {
                    "Abrir YouTube Music"
                } else {
                    "Sin reproducción activa"
                }
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
        SeekLine(
            progress = 0f,
            onSeek = {},
            enabled = false,
            showPosition = false,
        )
        BasicText(
            text = "--:--  /  --:--",
            style = workspaceMonoStyle(palette.contentMuted, 9),
            modifier = Modifier.padding(top = 5.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(top = 6.dp),
        ) {
            listOf("ANTERIOR", "REPRODUCIR", "SIGUIENTE").forEach { label ->
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
private fun SettingsPanel(
    onVeilSettingsSelected: () -> Unit,
    onSettingsSelected: (String) -> Unit,
) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            TileAction("Ajustes de Veil", onVeilSettingsSelected)
            TileAction("Todos los ajustes") { onSettingsSelected("settings") }
        }
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
private fun WorkSecondaryRow(
    compact: Boolean,
    notes: @Composable () -> Unit,
    pomodoro: @Composable () -> Unit,
) {
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            notes()
            pomodoro()
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(2f)) { notes() }
            Column(modifier = Modifier.weight(1f)) { pomodoro() }
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
private val YouTubeMusicPackages = listOf(
    "com.google.android.apps.youtube.music",
    "app.revanced.android.apps.youtube.music",
)

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
    CozyTile(
        label = "Tiempo",
        modifier = Modifier.fillMaxWidth().heightIn(min = SecondaryTileHeight),
    ) {
        when (weather.availability) {
            WeatherAvailability.NEEDS_PERMISSION -> TileAction("Usar ubicación aproximada", onPermissionRequested)
            WeatherAvailability.LOADING -> TileBody("Actualizando…")
            WeatherAvailability.UNAVAILABLE -> TileBody("Tiempo no disponible")
            WeatherAvailability.AVAILABLE -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    WeatherGlyph(weather.weatherCode, Modifier.size(58.dp))
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        TileTitle(
                            "${weather.temperatureCelsius?.roundToInt() ?: "—"}° · " +
                                weatherDescription(weather.weatherCode),
                        )
                        TileBody(
                            "Sensación ${weather.apparentTemperatureCelsius?.roundToInt() ?: "—"}°  ·  " +
                                "${weather.minimumCelsius?.roundToInt() ?: "—"}° / " +
                                "${weather.maximumCelsius?.roundToInt() ?: "—"}°" +
                                if (weather.isStale) " · desactualizado" else "",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkPomodoroTile(
    state: LauncherUiState,
    compact: Boolean,
    onStart: (Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
) {
    val focus = state.focusTimer
    var customMinutes by remember { mutableIntStateOf(25) }
    var showDialog by remember { mutableStateOf(false) }
    CozyTile(
        label = "Pomodoro",
        onClick = { showDialog = true },
        modifier = Modifier.fillMaxWidth().heightIn(
            min = if (compact) 112.dp else SecondaryTileHeight,
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
        FocusTimerStatus.IDLE -> "25m"
        FocusTimerStatus.COMPLETED -> "00:00"
        else -> formatDuration(focus.remainingMillis)
    }
    val status = when (focus.status) {
        FocusTimerStatus.IDLE -> "Elegir"
        FocusTimerStatus.RUNNING -> "En curso"
        FocusTimerStatus.PAUSED -> "En pausa"
        FocusTimerStatus.COMPLETED -> "Completado"
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
        FocusTimerStatus.IDLE -> "nuevo pomodoro"
        FocusTimerStatus.RUNNING -> "pomodoro en curso"
        FocusTimerStatus.PAUSED -> "pomodoro en pausa"
        FocusTimerStatus.COMPLETED -> "sesión completada"
    }
    RofiDialog(
        title = title,
        onDismiss = onDismiss,
        actions = {
            if (focus.status == FocusTimerStatus.RUNNING ||
                focus.status == FocusTimerStatus.PAUSED
            ) {
                RofiAction("finalizar", onFinish, danger = true)
            }
            Spacer(Modifier.weight(1f))
            RofiAction("cancelar", onDismiss)
            when (focus.status) {
                FocusTimerStatus.IDLE -> RofiAction("iniciar", { onStart(customMinutes) })
                FocusTimerStatus.RUNNING -> RofiAction("pausar", onPause)
                FocusTimerStatus.PAUSED -> RofiAction("reanudar", onResume)
                FocusTimerStatus.COMPLETED -> RofiAction("cerrar", onFinish)
            }
        },
    ) {
        when (focus.status) {
            FocusTimerStatus.IDLE -> {
                BasicText(
                    "duration:",
                    style = workspaceMonoStyle(LocalVeilPalette.current.accentActive, 9),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RofiAction("25 min", { onCustomMinutesChanged(25) })
                    RofiAction("50 min", { onCustomMinutesChanged(50) })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RofiAction("-5", { onCustomMinutesChanged(customMinutes - 5) })
                    BasicText(
                        "$customMinutes min",
                        style = workspaceMonoStyle(LocalVeilPalette.current.contentPrimary, 18),
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    RofiAction("+5", { onCustomMinutesChanged(customMinutes + 5) })
                }
                RofiBody(
                    "Veil puede solicitar notificaciones y alarmas exactas para avisarte " +
                        "incluso con la pantalla apagada.",
                )
            }
            FocusTimerStatus.RUNNING, FocusTimerStatus.PAUSED -> {
                BasicText(
                    formatDuration(focus.remainingMillis),
                    style = workspaceMonoStyle(LocalVeilPalette.current.contentPrimary, 28),
                )
                if (!focus.exactAlarmAvailable || !focus.notificationsAvailable) {
                    RofiBody("El aviso externo está limitado por los permisos actuales.")
                }
            }
            FocusTimerStatus.COMPLETED -> RofiBody("Tu sesión ha terminado.")
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
    in 71..77, 85, 86 -> "Nieve"
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
