package dev.vicent.veil.launcher.repository

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import dev.vicent.veil.launcher.model.FocusTimerState
import dev.vicent.veil.launcher.model.FocusTimerStatus
import dev.vicent.veil.launcher.SystemTimeProvider
import dev.vicent.veil.launcher.TimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusTimerRepository(
    context: Context,
    private val timeProvider: TimeProvider = SystemTimeProvider,
) {
    private val appContext = context.applicationContext
    private val store = FocusTimerStore(appContext, timeProvider)
    private val scheduler = FocusAlarmScheduler(appContext, timeProvider)
    private val mutableState = MutableStateFlow(readState())
    val state: StateFlow<FocusTimerState> = mutableState.asStateFlow()

    fun startObserving(scope: CoroutineScope) {
        scope.launch {
            while (isActive) {
                refreshFromStorage()
                delay(1_000)
            }
        }
    }

    fun start(durationMinutes: Int) {
        val duration = durationMinutes.coerceIn(5, 180) * 60_000L
        val endAt = timeProvider.currentTimeMillis() + duration
        store.write(FocusTimerStatus.RUNNING, duration, duration, endAt)
        scheduler.schedule(endAt)
        refreshFromStorage()
    }

    fun pause() {
        val current = readState()
        if (current.status != FocusTimerStatus.RUNNING) return
        scheduler.cancel()
        store.write(
            FocusTimerStatus.PAUSED,
            current.durationMillis,
            current.remainingMillis,
            0L,
        )
        refreshFromStorage()
    }

    fun resume() {
        val current = readState()
        if (current.status != FocusTimerStatus.PAUSED) return
        val endAt = timeProvider.currentTimeMillis() + current.remainingMillis
        store.write(
            FocusTimerStatus.RUNNING,
            current.durationMillis,
            current.remainingMillis,
            endAt,
        )
        scheduler.schedule(endAt)
        refreshFromStorage()
    }

    fun finish() {
        scheduler.cancel()
        store.write(
            FocusTimerStatus.IDLE,
            FocusTimerStore.DEFAULT_DURATION,
            FocusTimerStore.DEFAULT_DURATION,
            0L,
        )
        refreshFromStorage()
    }

    fun restoreScheduledAlarm() = scheduler.restore()

    private fun refreshFromStorage() {
        val current = readState()
        if (current.status == FocusTimerStatus.RUNNING && current.remainingMillis <= 0L) {
            scheduler.complete()
        }
        mutableState.value = readState()
    }

    private fun readState(): FocusTimerState = store.read(
        exactAlarmAvailable = scheduler.canScheduleExactAlarms(),
        notificationsAvailable = NotificationManagerCompat.from(appContext)
            .areNotificationsEnabled(),
    )
}
