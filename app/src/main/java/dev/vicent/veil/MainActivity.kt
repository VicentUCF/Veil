package dev.vicent.veil

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vicent.veil.config.AccentPalette
import dev.vicent.veil.config.LauncherConfig
import dev.vicent.veil.launcher.LauncherController
import dev.vicent.veil.launcher.repository.AmbientContinuityRepository
import dev.vicent.veil.launcher.repository.AppRepository
import dev.vicent.veil.launcher.repository.AudioMixerRepository
import dev.vicent.veil.launcher.repository.CalendarRepository
import dev.vicent.veil.launcher.repository.FocusTimerRepository
import dev.vicent.veil.launcher.repository.LauncherPreferencesRepository
import dev.vicent.veil.launcher.repository.QuickNotesRepository
import dev.vicent.veil.launcher.repository.SystemStatusRepository
import dev.vicent.veil.launcher.repository.SteamGameRepository
import dev.vicent.veil.launcher.repository.WeatherRepository
import dev.vicent.veil.launcher.system.AndroidAppLauncher
import dev.vicent.veil.launcher.system.AndroidClockLauncher
import dev.vicent.veil.launcher.system.AndroidSettingsLauncher
import dev.vicent.veil.launcher.system.AndroidWebLauncher
import dev.vicent.veil.launcher.system.LauncherAccessMonitor
import dev.vicent.veil.launcher.system.LauncherAccessCoordinator
import dev.vicent.veil.launcher.system.LauncherExternalActionCoordinator
import dev.vicent.veil.ui.LauncherAccessActions
import dev.vicent.veil.ui.LauncherAppActions
import dev.vicent.veil.ui.LauncherAppearanceActions
import dev.vicent.veil.ui.LauncherNavigationActions
import dev.vicent.veil.ui.LauncherScreen
import dev.vicent.veil.ui.LauncherWorkspaceActions
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
            steamGameRepository = SteamGameRepository(applicationContext),
            preferencesRepository = preferencesRepository,
            accessMonitor = accessMonitor,
            contexts = LauncherConfig.contexts,
            quickActionCount = LauncherConfig.QUICK_ACTION_COUNT,
            scope = lifecycleScope,
        )
    }

    private val appLauncher by lazy { AndroidAppLauncher(applicationContext) }
    private val clockLauncher by lazy { AndroidClockLauncher(applicationContext) }
    private val settingsLauncher by lazy { AndroidSettingsLauncher(applicationContext) }
    private val webLauncher by lazy { AndroidWebLauncher(applicationContext) }
    private var externalSurfaceLaunched = false
    private var hasCompletedFirstResume = false
    private var wasStoppedSinceLastResume = false
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
            controller.refreshApps()
        }
    }

    private val wallpaperReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            wallpaperRefreshRevision.value += 1
        }
    }

    private val accessCoordinator = LauncherAccessCoordinator(
        activity = this,
        controller = { controller },
        settingsLauncher = { settingsLauncher },
        onExternalSurfaceLaunched = { externalSurfaceLaunched = true },
    )
    private val externalActions = LauncherExternalActionCoordinator(
        controller = { controller },
        appLauncher = { appLauncher },
        settingsLauncher = { settingsLauncher },
        webLauncher = { webLauncher },
        clockLauncher = { clockLauncher },
        onExternalSurfaceLaunched = { externalSurfaceLaunched = true },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        hideStatusBar()

        controller.load()
        registerPackageReceiver()
        registerWallpaperReceiver()
        controller.refreshAccessState()

        setContent {
            val state by controller.state.collectAsStateWithLifecycle()
            val wallpaperRevision by wallpaperRefreshRevision.collectAsStateWithLifecycle()
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
                    navigationActions = LauncherNavigationActions(
                        onContextSelected = { index ->
                            controller.selectContext(index)
                            controller.refreshVisibleData()
                        },
                        onOpenDrawer = controller::openDrawer,
                        onCloseDrawer = controller::closeDrawer,
                        onOpenSettings = controller::openSettings,
                        onCloseSettings = controller::closeSettings,
                        onOpenMusicProviderPicker = controller::openMusicProviderPicker,
                        onOpenContextSlotPicker = controller::openContextSlotPicker,
                        onHomeButtonTap = {
                            externalActions.performHomeButtonAction(LauncherConfig.homeButton.onTap, state)
                        },
                        onHomeButtonLongPress = {
                            externalActions.performHomeButtonAction(
                                LauncherConfig.homeButton.onLongPress,
                                state,
                            )
                        },
                    ),
                    appActions = LauncherAppActions(
                        onAppSelected = externalActions::openApp,
                        onSettingsSelected = externalActions::openSetting,
                        onAppInfoSelected = externalActions::openAppInfo,
                        onAppUninstallSelected = externalActions::requestUninstall,
                        onExternalLinkSelected = externalActions::openExternalLink,
                        onPrivacyPolicySelected = externalActions::openPrivacyPolicy,
                        onSettingsAppSelected = controller::selectSettingsApp,
                        onMusicProviderCleared = controller::clearMusicProvider,
                        onContextSlotCleared = controller::clearContextSlot,
                    ),
                    accessActions = LauncherAccessActions(
                        onContinuityAccessRequested = accessCoordinator::openContinuityAccessSettings,
                        onContinuityOnboardingDismissed = controller::dismissContinuityOnboarding,
                        onCalendarPermissionRequested = accessCoordinator::requestCalendarPermission,
                        onLocationPermissionRequested = accessCoordinator::requestLocationPermission,
                        onAudioVisualizerPermissionRequested =
                            accessCoordinator::requestAudioVisualizerPermission,
                        onWallpaperSelected = {
                            externalActions.launchExternal(settingsLauncher::openWallpaperChooser)
                        },
                        onAppPermissionSettingsRequested = {
                            externalActions.launchExternal(settingsLauncher::openAppDetails)
                        },
                        onFocusNotificationsSelected = accessCoordinator::configureFocusNotifications,
                        onExactAlarmsSelected = {
                            externalActions.launchExternal(settingsLauncher::openExactAlarmSettings)
                        },
                        onDefaultHomeSelected = accessCoordinator::requestHomeRole,
                        onAndroidSettingsSelected = {
                            externalActions.launchExternal(settingsLauncher::openGeneralSettings)
                        },
                    ),
                    workspaceActions = LauncherWorkspaceActions(
                        onClockOpenRequested = externalActions::openClock,
                        onCalendarEventSelected = externalActions::openCalendarEvent,
                        onCalendarEventCreateRequested = externalActions::createCalendarEvent,
                        onCalendarOpenRequested = externalActions::openCalendar,
                        onGoogleCalendarConfigureRequested = externalActions::configureGoogleCalendar,
                        onContinuityAction = externalActions::performContinuityAction,
                        onHomeMediaDismissed = controller::dismissHomeMedia,
                        onAudioVolumeChanged = controller::setAudioVolume,
                        onFocusStartRequested = accessCoordinator::startFocusWithPermissions,
                        onFocusPause = controller::pauseFocus,
                        onFocusResume = controller::resumeFocus,
                        onFocusFinish = controller::finishFocus,
                        onQuickNoteAdded = controller::addQuickNote,
                        onQuickNoteUpdated = controller::updateQuickNote,
                        onQuickNoteDeleted = controller::deleteQuickNote,
                    ),
                    appearanceActions = LauncherAppearanceActions(
                        onAccentSelected = controller::setAccentMode,
                        onHomeTextToneSelected = controller::setHomeTextTone,
                        onHomeTextWeightSelected = controller::setHomeTextWeight,
                        onWallpaperScrimEnabledChanged = controller::setWallpaperScrimEnabled,
                        onWallpaperScrimIntensityChanged = controller::setWallpaperScrimIntensity,
                        onResetAppearance = controller::resetAppearance,
                    ),
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasCompletedFirstResume = true
        wasStoppedSinceLastResume = false
        controller.setAppVisible(true)
        externalSurfaceLaunched = false
        wallpaperRefreshRevision.value += 1
        controller.refreshAccessState()
        controller.restoreFocusAlarm()
        controller.refreshVisibleData()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            if (shouldHandleAsRepeatedHomePress(
                    externalSurfaceLaunched = externalSurfaceLaunched,
                    hasCompletedFirstResume = hasCompletedFirstResume,
                    wasStoppedSinceLastResume = wasStoppedSinceLastResume,
                )
            ) {
                controller.handleHomePressed()
            } else {
                controller.closeDrawer()
            }
            externalSurfaceLaunched = false
        }
    }

    override fun onPause() {
        controller.setAppVisible(false)
        super.onPause()
    }

    override fun onStop() {
        wasStoppedSinceLastResume = true
        super.onStop()
    }

    override fun onDestroy() {
        controller.setAppVisible(false)
        if (isPackageReceiverRegistered) unregisterReceiver(packageReceiver)
        if (isWallpaperReceiverRegistered) unregisterReceiver(wallpaperReceiver)
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
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

}

internal fun shouldHandleAsRepeatedHomePress(
    externalSurfaceLaunched: Boolean,
    hasCompletedFirstResume: Boolean,
    wasStoppedSinceLastResume: Boolean,
): Boolean = !externalSurfaceLaunched &&
    hasCompletedFirstResume &&
    !wasStoppedSinceLastResume
