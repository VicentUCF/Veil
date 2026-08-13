package dev.vicent.veil

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import dev.vicent.veil.config.LauncherConfig
import dev.vicent.veil.launcher.LauncherController
import dev.vicent.veil.launcher.LauncherUiState
import dev.vicent.veil.launcher.model.HomeButtonActionSpec
import dev.vicent.veil.launcher.repository.AmbientContinuityRepository
import dev.vicent.veil.launcher.repository.AppRepository
import dev.vicent.veil.launcher.repository.CalendarRepository
import dev.vicent.veil.launcher.repository.FocusTimerRepository
import dev.vicent.veil.launcher.repository.SystemStatusRepository
import dev.vicent.veil.launcher.repository.WeatherRepository
import dev.vicent.veil.launcher.system.AndroidAppLauncher
import dev.vicent.veil.launcher.system.AndroidSettingsLauncher
import dev.vicent.veil.ui.LauncherScreen
import dev.vicent.veil.ui.theme.VeilTheme

class MainActivity : ComponentActivity() {
    private val controller by lazy {
        LauncherController(
            appRepository = AppRepository(applicationContext),
            continuityRepository = AmbientContinuityRepository(applicationContext),
            calendarRepository = CalendarRepository(applicationContext),
            weatherRepository = WeatherRepository(applicationContext),
            focusTimerRepository = FocusTimerRepository(applicationContext),
            systemStatusRepository = SystemStatusRepository(applicationContext),
            contexts = LauncherConfig.contexts,
            quickActionCount = LauncherConfig.quickActionCount,
        )
    }

    private val appLauncher by lazy { AndroidAppLauncher(applicationContext) }
    private val settingsLauncher by lazy { AndroidSettingsLauncher(applicationContext) }
    private var requestExactAlarmAfterNotification = false
    private var externalSurfaceLaunched = false
    private var isLauncherResumed = false
    private var hasLauncherWindowFocus = false
    private var launcherPausedAtElapsedRealtime = Long.MIN_VALUE

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> controller.setCalendarAccessGranted(granted, lifecycleScope) }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> controller.setLocationAccessGranted(granted, lifecycleScope) }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        if (requestExactAlarmAfterNotification) requestExactAlarmAccess()
        requestExactAlarmAfterNotification = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        hideStatusBar()

        controller.load(lifecycleScope)
        controller.setContinuityAccessGranted(hasContinuityAccess())

        setContent {
            val state by controller.state.collectAsState()

            VeilTheme(palette = LauncherConfig.palette) {
                LauncherScreen(
                    state = state,
                    settingsShortcuts = settingsLauncher.shortcuts,
                    onContextSelected = { index ->
                        controller.selectContext(index)
                        controller.refreshVisibleData(lifecycleScope)
                    },
                    onContextStep = { direction ->
                        controller.stepContext(direction)
                        controller.refreshVisibleData(lifecycleScope)
                    },
                    onOpenDrawer = controller::openDrawer,
                    onCloseDrawer = controller::closeDrawer,
                    onAppSelected = { app ->
                        if (appLauncher.launch(app)) {
                            externalSurfaceLaunched = true
                            controller.closeDrawer()
                        } else {
                            controller.removeUnavailableApp(app.packageName)
                        }
                    },
                    onSettingsSelected = { shortcut ->
                        if (settingsLauncher.launch(shortcut)) {
                            externalSurfaceLaunched = true
                            controller.closeDrawer()
                        }
                    },
                    onAppInfoSelected = { app ->
                        if (appLauncher.openAppInfo(app)) {
                            externalSurfaceLaunched = true
                            controller.closeDrawer()
                        }
                    },
                    onAppUninstallSelected = { app ->
                        if (appLauncher.requestUninstall(app)) {
                            externalSurfaceLaunched = true
                            controller.closeDrawer()
                        }
                    },
                    onContinuityAccessRequested = ::openContinuityAccessSettings,
                    onCalendarPermissionRequested = {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    },
                    onLocationPermissionRequested = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    onCalendarEventSelected = { eventId ->
                        externalSurfaceLaunched = true
                        controller.openCalendarEvent(eventId)
                    },
                    onContinuityAction = { itemId, action, position ->
                        if (action == dev.vicent.veil.launcher.model.ContinuityAction.OPEN) {
                            externalSurfaceLaunched = true
                        }
                        controller.performContinuityAction(itemId, action, position)
                    },
                    onFocusStartRequested = ::startFocusWithPermissions,
                    onFocusPause = controller::pauseFocus,
                    onFocusResume = controller::resumeFocus,
                    onFocusFinish = controller::finishFocus,
                    onHomeButtonTap = {
                        performHomeButtonAction(LauncherConfig.homeButton.onTap, state)
                    },
                    onHomeButtonLongPress = {
                        performHomeButtonAction(LauncherConfig.homeButton.onLongPress, state)
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isLauncherResumed = true
        externalSurfaceLaunched = false
        controller.setContinuityAccessGranted(hasContinuityAccess())
        controller.setCalendarAccessGranted(hasPermission(Manifest.permission.READ_CALENDAR), lifecycleScope)
        controller.setLocationAccessGranted(hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION), lifecycleScope)
        controller.restoreFocusAlarm()
        controller.refreshVisibleData(lifecycleScope)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            val wasJustResumed = launcherPausedAtElapsedRealtime != Long.MIN_VALUE &&
                SystemClock.elapsedRealtime() - launcherPausedAtElapsedRealtime < 2_000L
            if (!externalSurfaceLaunched &&
                (isLauncherResumed || hasLauncherWindowFocus || wasJustResumed)
            ) {
                controller.handleHomePressed()
            } else {
                controller.closeDrawer()
            }
            externalSurfaceLaunched = false
        }
    }

    override fun onPause() {
        isLauncherResumed = false
        launcherPausedAtElapsedRealtime = SystemClock.elapsedRealtime()
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        hasLauncherWindowFocus = hasFocus
        if (hasFocus) hideStatusBar()
    }

    private fun hideStatusBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun hasContinuityAccess(): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)

    private fun openContinuityAccessSettings() {
        externalSurfaceLaunched = true
        runCatching { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            .recoverCatching { startActivity(Intent(Settings.ACTION_SETTINGS)) }
    }

    private fun startFocusWithPermissions(minutes: Int) {
        controller.startFocus(minutes)
        val needsNotifications = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        requestExactAlarmAfterNotification = needsNotifications
        if (needsNotifications) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            requestExactAlarmAccess()
        }
    }

    private fun requestExactAlarmAccess() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
        val alarmManager = getSystemService(AlarmManager::class.java)
        if (alarmManager.canScheduleExactAlarms()) return
        runCatching {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = android.net.Uri.parse("package:$packageName")
                },
            )
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun performHomeButtonAction(
        action: HomeButtonActionSpec,
        state: LauncherUiState,
    ) {
        when (action) {
            HomeButtonActionSpec.Everything -> controller.openDrawer()
            is HomeButtonActionSpec.App -> {
                val app = state.installedApps.firstOrNull { it.packageName == action.packageName }
                if (app != null && appLauncher.launch(app)) {
                    externalSurfaceLaunched = true
                } else {
                    app?.let { controller.removeUnavailableApp(it.packageName) }
                    controller.openDrawer()
                }
            }
            is HomeButtonActionSpec.Setting -> {
                val shortcut = settingsLauncher.shortcuts.firstOrNull { it.id == action.id }
                if (shortcut != null && settingsLauncher.launch(shortcut)) {
                    externalSurfaceLaunched = true
                } else {
                    controller.openDrawer()
                }
            }
        }
    }
}
