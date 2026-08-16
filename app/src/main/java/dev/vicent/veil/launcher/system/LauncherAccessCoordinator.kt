package dev.vicent.veil.launcher.system

import android.Manifest
import android.app.AlarmManager
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import dev.vicent.veil.launcher.LauncherController

class LauncherAccessCoordinator(
    private val activity: ComponentActivity,
    private val controller: () -> LauncherController,
    private val settingsLauncher: () -> AndroidSettingsLauncher,
    private val onExternalSurfaceLaunched: () -> Unit,
) {
    private var requestExactAlarmAfterNotification = false

    private val calendarPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> controller().setCalendarAccessGranted(granted) }

    private val locationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> controller().setLocationAccessGranted(granted) }

    private val notificationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        if (requestExactAlarmAfterNotification) requestExactAlarmAccess()
        requestExactAlarmAfterNotification = false
        controller().refreshAccessState()
    }

    private val audioVisualizerPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> controller().setAudioVisualizerPermissionGranted(granted) }

    private val homeRoleLauncher = activity.registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { controller().refreshAccessState() }

    fun requestCalendarPermission() {
        onExternalSurfaceLaunched()
        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
    }

    fun requestLocationPermission() {
        onExternalSurfaceLaunched()
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    fun requestAudioVisualizerPermission() {
        onExternalSurfaceLaunched()
        audioVisualizerPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    fun openContinuityAccessSettings(): Boolean =
        launchExternal(settingsLauncher()::openNotificationListenerSettings)

    fun configureFocusNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            return runCatching {
                onExternalSurfaceLaunched()
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                true
            }.getOrDefault(false)
        }
        return launchExternal(settingsLauncher()::openNotificationSettings)
    }

    fun requestHomeRole(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = activity.getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    return launchExternal(settingsLauncher()::openHomeSettings)
                }
                return runCatching {
                    onExternalSurfaceLaunched()
                    homeRoleLauncher.launch(
                        roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME),
                    )
                    true
                }.getOrDefault(false)
            }
        }
        return launchExternal(settingsLauncher()::openHomeSettings)
    }

    fun startFocusWithPermissions(minutes: Int) {
        controller().startFocus(minutes)
        val needsNotifications = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        requestExactAlarmAfterNotification = needsNotifications
        if (needsNotifications) {
            onExternalSurfaceLaunched()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestExactAlarmAccess()
        }
    }

    private fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val alarmManager = activity.getSystemService(AlarmManager::class.java)
        if (alarmManager.canScheduleExactAlarms()) return
        runCatching {
            onExternalSurfaceLaunched()
            activity.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = "package:${activity.packageName}".toUri()
                },
            )
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(activity, permission) ==
            PackageManager.PERMISSION_GRANTED

    private fun launchExternal(action: () -> Boolean): Boolean {
        val launched = action()
        if (launched) onExternalSurfaceLaunched()
        return launched
    }
}
