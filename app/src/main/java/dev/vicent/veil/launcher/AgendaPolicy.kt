package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.CalendarEventSummary
import java.util.Calendar

object AgendaPolicy {
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
}
