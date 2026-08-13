package dev.vicent.veil

import android.Manifest
import android.app.AlarmManager
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
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
import androidx.compose.runtime.remember
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import dev.vicent.veil.config.AccentPalette
import dev.vicent.veil.config.LauncherConfig
import dev.vicent.veil.launcher.LauncherController
import dev.vicent.veil.launcher.LauncherUiState
import dev.vicent.veil.launcher.model.HomeButtonActionSpec
import dev.vicent.veil.launcher.repository.AmbientContinuityRepository
import dev.vicent.veil.launcher.repository.AppRepository
import dev.vicent.veil.launcher.repository.AudioMixerRepository
import dev.vicent.veil.launcher.repository.CalendarRepository
import dev.vicent.veil.launcher.repository.FocusTimerRepository
import dev.vicent.veil.launcher.repository.LauncherPreferencesRepository
import dev.vicent.veil.launcher.repository.QuickNotesRepository
import dev.vicent.veil.launcher.repository.SystemStatusRepository
import dev.vicent.veil.launcher.repository.WeatherRepository
import dev.vicent.veil.launcher.system.AndroidAppLauncher
import dev.vicent.veil.launcher.system.AndroidClockLauncher
import dev.vicent.veil.launcher.system.AndroidSettingsLauncher
import dev.vicent.veil.launcher.system.LauncherAccessMonitor
import dev.vicent.veil.ui.LauncherScreen
import dev.vicent.veil.ui.theme.VeilTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val preferencesRepository by lazy { LauncherPreferencesRepository(applicationContext) }
    private val accessMonitor by lazy { LauncherAccessMonitor(applicationContext) }
    private val controller by lazy {
        LauncherController(
            appRepository = AppRepository(applicationContext),
            continuityRepository = AmbientContinuityRepository(applicationContext),
            calendarRepository = CalendarRepository(applicationContext),
            weatherRepository = WeatherRepository(applicationContext),
            focusTimerRepository = FocusTimerRepository(applicationContext),
            quickNotesRepository = QuickNotesRepository(applicationContext),
            systemStatusRepository = SystemStatusRepository(applicationContext),
            audioMixerRepository = AudioMixerRepository(applicationContext),
            preferencesRepository = preferencesRepository,
            accessMonitor = accessMonitor,
            contexts = LauncherConfig.contexts,
            quickActionCount = LauncherConfig.quickActionCount,
        )
    }

    private val appLauncher by lazy { AndroidAppLauncher(applicationContext) }
    private val clockLauncher by lazy { AndroidClockLauncher(applicationContext) }
    private val settingsLauncher by lazy { AndroidSettingsLauncher(applicationContext) }
    private var requestExactAlarmAfterNotification = false
    private var externalSurfaceLaunched = false
    private var isLauncherResumed = false
    private var hasLauncherWindowFocus = false
    private var launcherPausedAtElapsedRealtime = Long.MIN_VALUE
    private var isPackageReceiverRegistered = false
    private var isWallpaperReceiverRegistered = false
    private val wallpaperRefreshRevision = MutableStateFlow(0)

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_PACKAGE_REMOVED &&
                intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
            ) {
                return
            }
            controller.refreshApps(lifecycleScope)
        }
    }

    private val wallpaperReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            wallpaperRefreshRevision.value += 1
        }
    }

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
        controller.refreshAccessState(lifecycleScope)
    }

    private val audioVisualizerPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> controller.setAudioVisualizerPermissionGranted(granted) }

    private val homeRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        controller.refreshAccessState(lifecycleScope)
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
        registerPackageReceiver()
        registerWallpaperReceiver()
        controller.refreshAccessState(lifecycleScope)

        setContent {
            val state by controller.state.collectAsState()
            val wallpaperRevision by wallpaperRefreshRevision.collectAsState()
            val systemAccent = remember(wallpaperRevision) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    dynamicDarkColorScheme(this@MainActivity).primary
                } else {
                    null
                }
            }
            val palette = remember(state.preferences.accentMode, systemAccent) {
                AccentPalette.resolvePalette(
                    base = LauncherConfig.palette,
                    mode = state.preferences.accentMode,
                    systemAccent = systemAccent,
                )
            }

            VeilTheme(palette = palette) {
                LauncherScreen(
                    state = state,
                    systemAccent = systemAccent,
                    settingsShortcuts = settingsLauncher.shortcuts,
                    onContextSelected = { index ->
                        controller.selectContext(index)
                        controller.refreshVisibleData(lifecycleScope)
                    },
                    onOpenDrawer = controller::openDrawer,
                    onCloseDrawer = controller::closeDrawer,
                    onOpenSettings = controller::openSettings,
                    onCloseSettings = controller::closeSettings,
                    onOpenMusicProviderPicker = controller::openMusicProviderPicker,
                    onOpenContextSlotPicker = controller::openContextSlotPicker,
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
                    onContinuityOnboardingDismissed = controller::dismissContinuityOnboarding,
                    onCalendarPermissionRequested = {
                        calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
                    },
                    onLocationPermissionRequested = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    onClockOpenRequested = {
                        externalSurfaceLaunched = clockLauncher.openClock()
                    },
                    onCalendarEventSelected = { eventId ->
                        externalSurfaceLaunched = true
                        controller.openCalendarEvent(eventId)
                    },
                    onCalendarEventCreateRequested = {
                        externalSurfaceLaunched = controller.createCalendarEvent()
                    },
                    onCalendarOpenRequested = {
                        externalSurfaceLaunched = controller.openCalendar()
                    },
                    onGoogleCalendarConfigureRequested = {
                        externalSurfaceLaunched = controller.configureGoogleCalendar()
                    },
                    onContinuityAction = { itemId, action, position ->
                        if (action == dev.vicent.veil.launcher.model.ContinuityAction.OPEN) {
                            externalSurfaceLaunched = true
                        }
                        controller.performContinuityAction(itemId, action, position)
                    },
                    onHomeMediaDismissed = controller::dismissHomeMedia,
                    onAudioVisualizerPermissionRequested = {
                        audioVisualizerPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onAudioVolumeChanged = controller::setAudioVolume,
                    onFocusStartRequested = ::startFocusWithPermissions,
                    onFocusPause = controller::pauseFocus,
                    onFocusResume = controller::resumeFocus,
                    onFocusFinish = controller::finishFocus,
                    onQuickNoteAdded = controller::addQuickNote,
                    onQuickNoteUpdated = controller::updateQuickNote,
                    onQuickNoteDeleted = controller::deleteQuickNote,
                    onHomeButtonTap = {
                        performHomeButtonAction(LauncherConfig.homeButton.onTap, state)
                    },
                    onHomeButtonLongPress = {
                        performHomeButtonAction(LauncherConfig.homeButton.onLongPress, state)
                    },
                    onAccentSelected = controller::setAccentMode,
                    onWallpaperSelected = {
                        launchExternal(settingsLauncher::openWallpaperChooser)
                    },
                    onAppPermissionSettingsRequested = {
                        launchExternal(settingsLauncher::openAppDetails)
                    },
                    onFocusNotificationsSelected = ::configureFocusNotifications,
                    onExactAlarmsSelected = {
                        launchExternal(settingsLauncher::openExactAlarmSettings)
                    },
                    onDefaultHomeSelected = ::requestHomeRole,
                    onAndroidSettingsSelected = {
                        launchExternal(settingsLauncher::openGeneralSettings)
                    },
                    onResetAppearance = controller::resetAppearance,
                    onSettingsAppSelected = controller::selectSettingsApp,
                    onMusicProviderCleared = controller::clearMusicProvider,
                    onContextSlotCleared = controller::clearContextSlot,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        isLauncherResumed = true
        externalSurfaceLaunched = false
        wallpaperRefreshRevision.value += 1
        controller.refreshAccessState(lifecycleScope)
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

    override fun onDestroy() {
        if (isPackageReceiverRegistered) unregisterReceiver(packageReceiver)
        if (isWallpaperReceiverRegistered) unregisterReceiver(wallpaperReceiver)
        super.onDestroy()
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

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            this,
            packageReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
        isPackageReceiverRegistered = true
    }

    private fun registerWallpaperReceiver() {
        ContextCompat.registerReceiver(
            this,
            wallpaperReceiver,
            IntentFilter(Intent.ACTION_WALLPAPER_CHANGED),
            ContextCompat.RECEIVER_EXPORTED,
        )
        isWallpaperReceiverRegistered = true
    }

    private fun openContinuityAccessSettings(): Boolean =
        launchExternal(settingsLauncher::openNotificationListenerSettings)

    private fun launchExternal(action: () -> Boolean): Boolean {
        val launched = action()
        if (launched) externalSurfaceLaunched = true
        return launched
    }

    private fun configureFocusNotifications(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        ) {
            return runCatching {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                true
            }.getOrDefault(false)
        }
        return launchExternal(settingsLauncher::openNotificationSettings)
    }

    private fun requestHomeRole(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    return launchExternal(settingsLauncher::openHomeSettings)
                }
                return runCatching {
                    externalSurfaceLaunched = true
                    homeRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                    true
                }.getOrDefault(false)
            }
        }
        return launchExternal(settingsLauncher::openHomeSettings)
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
