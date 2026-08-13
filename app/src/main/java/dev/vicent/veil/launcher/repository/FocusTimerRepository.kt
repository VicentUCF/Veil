package dev.vicent.veil.launcher.repository

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.vicent.veil.MainActivity
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.FocusTimerState
import dev.vicent.veil.launcher.model.FocusTimerStatus
import dev.vicent.veil.launcher.WorkspaceDataPolicy
import dev.vicent.veil.launcher.system.FocusAlarmReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusTimerRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)
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
        val endAt = System.currentTimeMillis() + duration
        write(FocusTimerStatus.RUNNING, duration, duration, endAt)
        schedule(endAt)
        refreshFromStorage()
    }

    fun pause() {
        val state = readState()
        if (state.status != FocusTimerStatus.RUNNING) return
        cancelAlarm()
        write(FocusTimerStatus.PAUSED, state.durationMillis, state.remainingMillis, 0L)
        refreshFromStorage()
    }

    fun resume() {
        val state = readState()
        if (state.status != FocusTimerStatus.PAUSED) return
        val endAt = System.currentTimeMillis() + state.remainingMillis
        write(FocusTimerStatus.RUNNING, state.durationMillis, state.remainingMillis, endAt)
        schedule(endAt)
        refreshFromStorage()
    }

    fun finish() {
        cancelAlarm()
        write(FocusTimerStatus.IDLE, DEFAULT_DURATION, DEFAULT_DURATION, 0L)
        refreshFromStorage()
    }

    fun restoreScheduledAlarm() = restore(appContext)

    private fun refreshFromStorage() {
        val state = readState()
        if (state.status == FocusTimerStatus.RUNNING && state.remainingMillis <= 0L) {
            complete(appContext)
        }
        mutableState.value = readState()
    }

    private fun readState(): FocusTimerState {
        val status = runCatching {
            FocusTimerStatus.valueOf(preferences.getString(KEY_STATUS, FocusTimerStatus.IDLE.name).orEmpty())
        }.getOrDefault(FocusTimerStatus.IDLE)
        val duration = preferences.getLong(KEY_DURATION, DEFAULT_DURATION).coerceAtLeast(5 * 60_000L)
        val storedRemaining = preferences.getLong(KEY_REMAINING, duration)
        val endAt = preferences.getLong(KEY_END_AT, 0L)
        val remaining = WorkspaceDataPolicy.focusRemainingMillis(
            status,
            endAt,
            storedRemaining,
            System.currentTimeMillis(),
        )
        return FocusTimerState(
            status = status,
            durationMillis = duration,
            remainingMillis = remaining,
            exactAlarmAvailable = canScheduleExactAlarms(),
            notificationsAvailable = NotificationManagerCompat.from(appContext).areNotificationsEnabled(),
        )
    }

    private fun write(status: FocusTimerStatus, duration: Long, remaining: Long, endAt: Long) {
        preferences.edit()
            .putString(KEY_STATUS, status.name)
            .putLong(KEY_DURATION, duration)
            .putLong(KEY_REMAINING, remaining)
            .putLong(KEY_END_AT, endAt)
            .apply()
    }

    private fun schedule(endAt: Long) {
        val pendingIntent = alarmPendingIntent(appContext)
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pendingIntent)
        }
    }

    private fun cancelAlarm() = alarmManager.cancel(alarmPendingIntent(appContext))

    private fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    companion object {
        private const val PREFERENCES = "veil_focus"
        private const val KEY_STATUS = "status"
        private const val KEY_DURATION = "duration"
        private const val KEY_REMAINING = "remaining"
        private const val KEY_END_AT = "end_at"
        private const val DEFAULT_DURATION = 25 * 60_000L
        private const val CHANNEL_ID = "focus_completion"
        private const val NOTIFICATION_ID = 2501

        fun restore(context: Context) {
            val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            if (preferences.getString(KEY_STATUS, null) != FocusTimerStatus.RUNNING.name) return
            val endAt = preferences.getLong(KEY_END_AT, 0L)
            if (endAt <= System.currentTimeMillis()) {
                complete(context)
                return
            }
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
            if (exact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    endAt,
                    alarmPendingIntent(context),
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    endAt,
                    alarmPendingIntent(context),
                )
            }
        }

        fun complete(context: Context) {
            val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            val duration = preferences.getLong(KEY_DURATION, DEFAULT_DURATION)
            preferences.edit()
                .putString(KEY_STATUS, FocusTimerStatus.COMPLETED.name)
                .putLong(KEY_DURATION, duration)
                .putLong(KEY_REMAINING, 0L)
                .putLong(KEY_END_AT, 0L)
                .apply()
            notifyCompletion(context)
        }

        private fun alarmPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            2501,
            Intent(context, FocusAlarmReceiver::class.java).setAction(FocusAlarmReceiver.ACTION_COMPLETE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun notifyCompletion(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "Focus", NotificationManager.IMPORTANCE_HIGH),
                )
            }
            val contentIntent = PendingIntent.getActivity(
                context,
                2502,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Focus completado")
                .setContentText("Tu sesión ha terminado.")
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
            val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
            if (notificationsGranted) {
                runCatching {
                    NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
                }
            }
        }
    }
}
