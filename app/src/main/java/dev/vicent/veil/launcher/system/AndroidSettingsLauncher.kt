package dev.vicent.veil.launcher.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.SettingsShortcut

class AndroidSettingsLauncher(private val context: Context) {
    private val actionsById = mapOf(
        "settings" to Settings.ACTION_SETTINGS,
        "network" to Settings.ACTION_WIRELESS_SETTINGS,
        "bluetooth" to Settings.ACTION_BLUETOOTH_SETTINGS,
        "display" to Settings.ACTION_DISPLAY_SETTINGS,
        "sound" to Settings.ACTION_SOUND_SETTINGS,
        "applications" to Settings.ACTION_APPLICATION_SETTINGS,
        "storage" to Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
        "battery" to Settings.ACTION_BATTERY_SAVER_SETTINGS,
        "security" to Settings.ACTION_SECURITY_SETTINGS,
        "device_info" to Settings.ACTION_DEVICE_INFO_SETTINGS,
        "language" to Settings.ACTION_LOCALE_SETTINGS,
        "accessibility" to Settings.ACTION_ACCESSIBILITY_SETTINGS,
    )
    val shortcuts = listOf(
        shortcut("settings", R.string.shortcut_settings, R.string.shortcut_settings_search),
        shortcut("network", R.string.shortcut_network, R.string.shortcut_network_search),
        shortcut("bluetooth", R.string.shortcut_bluetooth, R.string.shortcut_bluetooth_search),
        shortcut("display", R.string.shortcut_display, R.string.shortcut_display_search),
        shortcut("sound", R.string.shortcut_sound, R.string.shortcut_sound_search),
        shortcut(
            "applications",
            R.string.shortcut_applications,
            R.string.shortcut_applications_search,
        ),
        shortcut("storage", R.string.shortcut_storage, R.string.shortcut_storage_search),
        shortcut("battery", R.string.shortcut_battery, R.string.shortcut_battery_search),
        shortcut("security", R.string.shortcut_security, R.string.shortcut_security_search),
        shortcut(
            "device_info",
            R.string.shortcut_device_info,
            R.string.shortcut_device_info_search,
        ),
        shortcut("language", R.string.shortcut_language, R.string.shortcut_language_search),
        shortcut(
            "accessibility",
            R.string.shortcut_accessibility,
            R.string.shortcut_accessibility_search,
        ),
    )

    fun launch(shortcut: SettingsShortcut): Boolean {
        val action = actionsById[shortcut.id] ?: return false
        return start(Intent(action)) || start(Intent(Settings.ACTION_SETTINGS))
    }

    fun openWallpaperChooser(): Boolean =
        start(Intent(Intent.ACTION_SET_WALLPAPER)) ||
            start(Intent(Settings.ACTION_DISPLAY_SETTINGS)) ||
            openGeneralSettings()

    fun openAppDetails(): Boolean =
        start(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
            },
        ) || start(Intent(Settings.ACTION_APPLICATION_SETTINGS)) || openGeneralSettings()

    fun openNotificationSettings(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return openAppDetails()
        return start(
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            },
        ) || openAppDetails()
    }

    fun openNotificationListenerSettings(): Boolean =
        start(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) || openGeneralSettings()

    fun openHomeSettings(): Boolean =
        start(Intent(Settings.ACTION_HOME_SETTINGS)) || openGeneralSettings()

    fun openExactAlarmSettings(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return start(
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:${context.packageName}".toUri()
            },
        ) || openAppDetails()
    }

    fun openGeneralSettings(): Boolean = start(Intent(Settings.ACTION_SETTINGS))

    private fun shortcut(id: String, label: Int, searchTerms: Int) = SettingsShortcut(
        id = id,
        label = context.getString(label),
        searchTerms = context.getString(searchTerms),
    )

    private fun start(intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
