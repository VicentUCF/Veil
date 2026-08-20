package dev.vicent.veil.launcher.system

import dev.vicent.veil.launcher.LauncherController
import dev.vicent.veil.launcher.LauncherUiState
import dev.vicent.veil.launcher.model.HomeButtonActionSpec
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.SettingsShortcut

class LauncherExternalActionCoordinator(
    private val controller: () -> LauncherController,
    private val appLauncher: () -> AndroidAppLauncher,
    private val settingsLauncher: () -> AndroidSettingsLauncher,
    private val webLauncher: () -> AndroidWebLauncher,
    private val clockLauncher: () -> AndroidClockLauncher,
    private val privacyPolicyUrl: String,
    private val onExternalSurfaceLaunched: () -> Unit,
    private val onPackageInventoryMayChange: () -> Unit,
    private val onCalendarMayChange: () -> Unit,
) {
    fun openApp(app: LauncherApp) {
        if (appLauncher().launch(app)) {
            markLaunched()
            controller().closeDrawer()
        } else {
            controller().removeUnavailableApp(app.packageName)
        }
    }

    fun openSetting(shortcut: SettingsShortcut) {
        if (settingsLauncher().launch(shortcut)) {
            markLaunched()
            controller().closeDrawer()
        }
    }

    fun openAppInfo(app: LauncherApp) {
        if (appLauncher().openAppInfo(app)) {
            onPackageInventoryMayChange()
            markLaunched()
            controller().closeDrawer()
        }
    }

    fun requestUninstall(app: LauncherApp) {
        if (appLauncher().requestUninstall(app)) {
            onPackageInventoryMayChange()
            markLaunched()
            controller().closeDrawer()
        }
    }

    fun openExternalLink(url: String) {
        launchExternal { webLauncher().open(url) }
    }

    fun openPrivacyPolicy(): Boolean = launchExternal {
        webLauncher().openPrivacyPolicy(privacyPolicyUrl)
    }

    fun openClock() {
        launchExternal(clockLauncher()::openClock)
    }

    fun openCalendarEvent(eventId: Long) {
        markLaunched()
        controller().openCalendarEvent(eventId)
    }

    fun createCalendarEvent() {
        if (launchExternal(controller()::createCalendarEvent)) onCalendarMayChange()
    }

    fun openCalendar() {
        launchExternal(controller()::openCalendar)
    }

    fun configureGoogleCalendar() {
        launchExternal(controller()::configureGoogleCalendar)
    }

    fun performContinuityAction(
        itemId: String,
        action: ContinuityAction,
        positionMillis: Long?,
    ) {
        if (action == ContinuityAction.OPEN) markLaunched()
        controller().performContinuityAction(itemId, action, positionMillis)
    }

    fun performHomeButtonAction(action: HomeButtonActionSpec, state: LauncherUiState) {
        when (action) {
            HomeButtonActionSpec.Everything -> controller().openDrawer()
            is HomeButtonActionSpec.App -> {
                val appsByPackage = state.installedApps.associateBy(LauncherApp::packageName)
                val app = action.packageCandidates.firstNotNullOfOrNull(appsByPackage::get)
                if (app != null && appLauncher().launch(app)) {
                    markLaunched()
                } else {
                    app?.let { controller().removeUnavailableApp(it.packageName) }
                    controller().openDrawer()
                }
            }
            is HomeButtonActionSpec.Setting -> {
                val shortcut = settingsLauncher().shortcuts.firstOrNull { it.id == action.id }
                if (shortcut != null && settingsLauncher().launch(shortcut)) {
                    markLaunched()
                } else {
                    controller().openDrawer()
                }
            }
        }
    }

    fun launchExternal(action: () -> Boolean): Boolean {
        val launched = action()
        if (launched) markLaunched()
        return launched
    }

    private fun markLaunched() = onExternalSurfaceLaunched()
}
