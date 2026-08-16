package dev.vicent.veil.launcher.repository

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.vicent.veil.MainActivity
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.FocusTimerStatus
import dev.vicent.veil.launcher.system.FocusAlarmReceiver

internal class FocusAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val store = FocusTimerStore(context)

    fun schedule(endAt: Long) {
        val pendingIntent = alarmPendingIntent()
        if (canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAt, pendingIntent)
        }
    }

    fun cancel() = alarmManager.cancel(alarmPendingIntent())

    fun canScheduleExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    fun restore() {
        if (store.persistedStatus() != FocusTimerStatus.RUNNING) return
        val endAt = store.persistedEndAt()
        if (endAt <= System.currentTimeMillis()) {
            complete()
        } else {
            schedule(endAt)
        }
    }

    fun complete() {
        val duration = store.persistedDuration()
        store.write(FocusTimerStatus.COMPLETED, duration, 0L, 0L)
        notifyCompletion()
    }

    private fun alarmPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        2501,
        Intent(context, FocusAlarmReceiver::class.java).setAction(FocusAlarmReceiver.ACTION_COMPLETE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notifyCompletion() {
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
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
        }
    }

    private companion object {
        const val CHANNEL_ID = "focus_completion"
        const val NOTIFICATION_ID = 2501
    }
}
