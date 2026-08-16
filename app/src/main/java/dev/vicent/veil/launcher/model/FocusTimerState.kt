package dev.vicent.veil.launcher.model

enum class FocusTimerStatus { IDLE, RUNNING, PAUSED, COMPLETED }

data class FocusTimerState(
    val status: FocusTimerStatus = FocusTimerStatus.IDLE,
    val durationMillis: Long = 25 * 60 * 1000L,
    val remainingMillis: Long = 25 * 60 * 1000L,
    val exactAlarmAvailable: Boolean = true,
    val notificationsAvailable: Boolean = true,
)
