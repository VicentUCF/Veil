package dev.vicent.veil.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.vicent.veil.R
import dev.vicent.veil.launcher.AgendaPolicy
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.model.QuickNoteChecklistItem
import dev.vicent.veil.launcher.model.QuickNoteType
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
internal fun WorkWorkspace(
    state: WorkWorkspaceUiState,
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
        AgendaPolicy.workEvents(state.calendarEvents, System.currentTimeMillis())
    }
    var agendaDialog by remember { mutableStateOf<AgendaDialogMode?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(WorkspaceLayoutTokens.SECTION_SPACING)) {
        CozyTile(
            label = stringResource(R.string.agenda_tile_label),
            prominent = true,
            onClick = { agendaDialog = AgendaDialogMode.ACTIONS },
            modifier = Modifier.fillMaxWidth().heightIn(
                min = WorkspaceLayoutTokens.PRIMARY_TILE_HEIGHT,
            ),
        ) {
            if (!state.calendarAccessGranted) {
                TileAction(stringResource(R.string.agenda_connect), onCalendarPermissionRequested)
            } else if (workEvents.isEmpty()) {
                TileTitle(stringResource(R.string.agenda_empty_title))
                TileBody(stringResource(R.string.agenda_empty_body))
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
                focus = state.focusTimer,
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
        text = if (progress.isComplete) {
            stringResource(R.string.work_progress_completed)
        } else {
            stringResource(R.string.work_progress_running)
        },
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
        TileAction(stringResource(R.string.work_resume)) {
            onContinuityAction(progress.id, ContinuityAction.OPEN, null)
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
        Column(verticalArrangement = Arrangement.spacedBy(WorkspaceLayoutTokens.SECTION_SPACING)) {
            notes()
            pomodoro()
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(WorkspaceLayoutTokens.SECTION_SPACING)) {
            Column(modifier = Modifier.weight(2f)) { notes() }
            Column(modifier = Modifier.weight(1f)) { pomodoro() }
        }
    }
}
