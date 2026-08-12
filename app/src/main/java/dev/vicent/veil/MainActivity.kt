package dev.vicent.veil

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import dev.vicent.veil.config.LauncherConfig
import dev.vicent.veil.launcher.LauncherController
import dev.vicent.veil.launcher.repository.AppRepository
import dev.vicent.veil.launcher.system.AndroidAppLauncher
import dev.vicent.veil.launcher.system.AndroidSettingsLauncher
import dev.vicent.veil.ui.LauncherScreen
import dev.vicent.veil.ui.theme.VeilTheme

class MainActivity : ComponentActivity() {
    private val controller by lazy {
        LauncherController(
            appRepository = AppRepository(applicationContext),
            contexts = LauncherConfig.contexts,
            automaticHomeAppCount = LauncherConfig.automaticHomeAppCount,
        )
    }

    private val appLauncher by lazy { AndroidAppLauncher(applicationContext) }
    private val settingsLauncher by lazy { AndroidSettingsLauncher(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        hideStatusBar()

        controller.load(lifecycleScope)

        setContent {
            val state by controller.state.collectAsState()

            VeilTheme(palette = LauncherConfig.palette) {
                LauncherScreen(
                    state = state,
                    settingsShortcuts = settingsLauncher.shortcuts,
                    onContextSelected = controller::selectContext,
                    onContextStep = controller::stepContext,
                    onOpenDrawer = controller::openDrawer,
                    onCloseDrawer = controller::closeDrawer,
                    onAppSelected = { app ->
                        if (appLauncher.launch(app)) {
                            controller.closeDrawer()
                        } else {
                            controller.removeUnavailableApp(app.packageName)
                        }
                    },
                    onSettingsSelected = { shortcut ->
                        if (settingsLauncher.launch(shortcut)) {
                            controller.closeDrawer()
                        }
                    },
                    onAppInfoSelected = { app ->
                        if (appLauncher.openAppInfo(app)) {
                            controller.closeDrawer()
                        }
                    },
                    onAppUninstallSelected = { app ->
                        if (appLauncher.requestUninstall(app)) {
                            controller.closeDrawer()
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            controller.toggleDrawer()
        }
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
}
