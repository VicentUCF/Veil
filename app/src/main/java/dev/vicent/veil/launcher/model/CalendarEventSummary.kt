package dev.vicent.veil.launcher.model

data class CalendarEventSummary(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
)
