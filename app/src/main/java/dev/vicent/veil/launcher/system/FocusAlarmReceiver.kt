package dev.vicent.veil.launcher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.vicent.veil.launcher.repository.FocusAlarmScheduler

class FocusAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = FocusAlarmScheduler(context.applicationContext)
        when (intent.action) {
            ACTION_COMPLETE -> scheduler.complete()
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> scheduler.restore()
        }
    }

    companion object {
        const val ACTION_COMPLETE = "dev.vicent.veil.action.FOCUS_COMPLETE"
    }
}
