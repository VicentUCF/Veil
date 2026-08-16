package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.FocusTimerStatus

object FocusTimerPolicy {
    fun remainingMillis(
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
