package dev.vicent.veil.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.vicent.veil.launcher.model.CalendarEventSummary
import dev.vicent.veil.ui.theme.LocalVeilPalette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal enum class AgendaDialogMode { ACTIONS, WEEK }

@Composable
internal fun AgendaRofiDialog(
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
internal fun EventRow(event: CalendarEventSummary, onSelected: (Long) -> Unit) {
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
