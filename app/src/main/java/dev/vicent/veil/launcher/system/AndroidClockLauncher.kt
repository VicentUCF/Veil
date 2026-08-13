package dev.vicent.veil.launcher.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.AlarmClock

class AndroidClockLauncher(private val context: Context) {
    fun openClock(): Boolean {
        val showAlarms = Intent(AlarmClock.ACTION_SHOW_ALARMS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (start(showAlarms)) return true

        return context.packageManager
            .queryIntentActivities(showAlarms, PackageManager.MATCH_DEFAULT_ONLY)
            .asSequence()
            .mapNotNull { result ->
                context.packageManager.getLaunchIntentForPackage(result.activityInfo.packageName)
            }
            .any(::start)
    }

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
