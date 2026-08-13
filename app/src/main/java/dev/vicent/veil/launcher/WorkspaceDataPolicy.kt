package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.CalendarEventSummary
import dev.vicent.veil.launcher.model.FocusTimerStatus
import dev.vicent.veil.launcher.model.LauncherContextKind
import java.util.Calendar

object WorkspaceDataPolicy {
    fun showsContextDock(kind: LauncherContextKind): Boolean =
        kind != LauncherContextKind.CURRENT

    fun workEvents(
        events: List<CalendarEventSummary>,
        nowMillis: Long,
    ): List<CalendarEventSummary> {
        if (events.isEmpty()) return emptyList()
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val eventCalendar = Calendar.getInstance()
        val today = events.filter { event ->
            eventCalendar.timeInMillis = event.startMillis
            eventCalendar.get(Calendar.ERA) == now.get(Calendar.ERA) &&
                eventCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                eventCalendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        }
        return (today.ifEmpty { events.take(1) }).take(3)
    }

    fun focusRemainingMillis(
        status: FocusTimerStatus,
        endAtMillis: Long,
        storedRemainingMillis: Long,
        nowMillis: Long,
    ): Long = if (status == FocusTimerStatus.RUNNING) {
        (endAtMillis - nowMillis).coerceAtLeast(0L)
    } else {
        storedRemainingMillis.coerceAtLeast(0L)
    }
}
