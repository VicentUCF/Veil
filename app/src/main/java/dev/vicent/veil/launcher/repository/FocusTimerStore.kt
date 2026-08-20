package dev.vicent.veil.launcher.repository

import android.content.Context
import androidx.core.content.edit
import dev.vicent.veil.launcher.FocusTimerPolicy
import dev.vicent.veil.launcher.SystemTimeProvider
import dev.vicent.veil.launcher.TimeProvider
import dev.vicent.veil.launcher.model.FocusTimerState
import dev.vicent.veil.launcher.model.FocusTimerStatus

internal class FocusTimerStore(
    context: Context,
    private val timeProvider: TimeProvider = SystemTimeProvider,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(exactAlarmAvailable: Boolean, notificationsAvailable: Boolean): FocusTimerState {
        val status = runCatching {
            FocusTimerStatus.valueOf(
                preferences.getString(KEY_STATUS, FocusTimerStatus.IDLE.name).orEmpty(),
            )
        }.getOrDefault(FocusTimerStatus.IDLE)
        val duration = preferences.getLong(KEY_DURATION, DEFAULT_DURATION)
            .coerceAtLeast(5 * 60_000L)
        val remaining = FocusTimerPolicy.remainingMillis(
            status = status,
            endAtMillis = preferences.getLong(KEY_END_AT, 0L),
            storedRemainingMillis = preferences.getLong(KEY_REMAINING, duration),
            nowMillis = timeProvider.currentTimeMillis(),
        )
        return FocusTimerState(
            status = status,
            durationMillis = duration,
            remainingMillis = remaining,
            exactAlarmAvailable = exactAlarmAvailable,
            notificationsAvailable = notificationsAvailable,
        )
    }

    fun write(status: FocusTimerStatus, duration: Long, remaining: Long, endAt: Long) {
        preferences.edit {
            putString(KEY_STATUS, status.name)
            putLong(KEY_DURATION, duration)
            putLong(KEY_REMAINING, remaining)
            putLong(KEY_END_AT, endAt)
        }
    }

    fun persistedStatus(): FocusTimerStatus? = preferences.getString(KEY_STATUS, null)
        ?.let { runCatching { FocusTimerStatus.valueOf(it) }.getOrNull() }

    fun persistedEndAt(): Long = preferences.getLong(KEY_END_AT, 0L)

    fun persistedDuration(): Long = preferences.getLong(KEY_DURATION, DEFAULT_DURATION)

    companion object {
        const val DEFAULT_DURATION = 25 * 60_000L
        const val PREFERENCES_NAME = "veil_focus"
        private const val KEY_STATUS = "status"
        private const val KEY_DURATION = "duration"
        private const val KEY_REMAINING = "remaining"
        private const val KEY_END_AT = "end_at"
    }
}
