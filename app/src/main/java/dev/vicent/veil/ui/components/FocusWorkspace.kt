package dev.vicent.veil.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.vicent.veil.R
import dev.vicent.veil.launcher.AgendaPolicy
import dev.vicent.veil.launcher.model.QuickNoteChecklistItem
import dev.vicent.veil.launcher.model.QuickNoteType

@Composable
internal fun FocusWorkspace(
    state: FocusWorkspaceUiState,
    compact: Boolean,
    onCalendarPermissionRequested: () -> Unit,
    onCalendarEventSelected: (Long) -> Unit,
    onCalendarEventCreateRequested: () -> Unit,
    onCalendarOpenRequested: () -> Unit,
    onGoogleCalendarConfigureRequested: () -> Unit,
    onFocusStart: (Int) -> Unit,
    onFocusPause: () -> Unit,
    onFocusResume: () -> Unit,
    onFocusFinish: () -> Unit,
    onQuickNoteAdded: (String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onQuickNoteUpdated: (Long, String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onQuickNoteDeleted: (Long) -> Unit,
) {
    val nextEvent = remember(state.calendarEvents) {
        AgendaPolicy.workEvents(state.calendarEvents, System.currentTimeMillis()).firstOrNull()
    }
    var agendaDialog by remember { mutableStateOf<AgendaDialogMode?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(WorkspaceLayoutTokens.SECTION_SPACING)) {
        WorkPomodoroTile(
            focus = state.focusTimer,
            compact = compact,
            primary = true,
            onStart = onFocusStart,
            onPause = onFocusPause,
            onResume = onFocusResume,
            onFinish = onFocusFinish,
        )
        ResponsivePair(
            compact = compact,
            left = {
                CozyTile(
                    label = stringResource(R.string.focus_workspace_agenda),
                    onClick = { agendaDialog = AgendaDialogMode.ACTIONS },
                    modifier = Modifier.fillMaxWidth().heightIn(
                        min = WorkspaceLayoutTokens.SECONDARY_TILE_HEIGHT,
                    ),
                ) {
                    when {
                        !state.calendarAccessGranted -> TileAction(
                            stringResource(R.string.agenda_connect),
                            onCalendarPermissionRequested,
                        )
                        nextEvent == null -> TileBody(
                            stringResource(R.string.focus_workspace_no_event),
                        )
                        else -> EventRow(nextEvent, onCalendarEventSelected)
                    }
                }
            },
            right = {
                WorkQuickNotesTile(
                    notes = state.quickNotes,
                    onAdd = onQuickNoteAdded,
                    onUpdate = onQuickNoteUpdated,
                    onDelete = onQuickNoteDeleted,
                )
            },
        )
    }

    agendaDialog?.let { mode ->
        AgendaRofiDialog(
            mode = mode,
            events = state.calendarEvents,
            accessGranted = state.calendarAccessGranted,
            onDismiss = { agendaDialog = null },
            onBack = { agendaDialog = AgendaDialogMode.ACTIONS },
            onShowWeek = { agendaDialog = AgendaDialogMode.WEEK },
            onPermissionRequested = { agendaDialog = null; onCalendarPermissionRequested() },
            onCreateEvent = { agendaDialog = null; onCalendarEventCreateRequested() },
            onOpenCalendar = { agendaDialog = null; onCalendarOpenRequested() },
            onConfigureGoogle = { agendaDialog = null; onGoogleCalendarConfigureRequested() },
            onEventSelected = { id -> agendaDialog = null; onCalendarEventSelected(id) },
        )
    }
}
