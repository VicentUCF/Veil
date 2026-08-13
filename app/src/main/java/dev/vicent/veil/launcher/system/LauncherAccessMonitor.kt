package dev.vicent.veil.launcher.system

import android.Manifest
import android.app.AlarmManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.vicent.veil.launcher.model.LauncherAccessState

class LauncherAccessMonitor(context: Context) {
    private val appContext = context.applicationContext

    fun snapshot(): LauncherAccessState = LauncherAccessState(
        isDefaultHome = isDefaultHome(),
        continuityGranted = NotificationManagerCompat
            .getEnabledListenerPackages(appContext)
            .contains(appContext.packageName),
        calendarGranted = hasPermission(Manifest.permission.READ_CALENDAR),
        approximateLocationGranted = hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION),
        audioVisualizerGranted = hasPermission(Manifest.permission.RECORD_AUDIO),
        focusNotificationsGranted = NotificationManagerCompat.from(appContext)
            .areNotificationsEnabled() && (
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    hasPermission(Manifest.permission.POST_NOTIFICATIONS)
                ),
        exactAlarmsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            appContext.getSystemService(AlarmManager::class.java).canScheduleExactAlarms(),
    )

    private fun isDefaultHome(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = appContext.getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            }
        }
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return appContext.packageManager.resolveActivity(
            homeIntent,
            PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo?.packageName == appContext.packageName
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED
}
