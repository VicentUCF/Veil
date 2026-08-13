package dev.vicent.veil.launcher.model

enum class AccentMode(val persistedValue: String) {
    VEIL("veil"),
    AMBER("amber"),
    SAGE("sage"),
    SKY("sky"),
    LILAC("lilac"),
    SYSTEM("system");

    companion object {
        fun fromPersistedValue(value: String?): AccentMode =
            entries.firstOrNull { it.persistedValue == value } ?: VEIL
    }
}

data class LauncherPreferences(
    val accentMode: AccentMode = AccentMode.VEIL,
)

object LauncherPreferencesPolicy {
    fun decodeAccent(value: String?): LauncherPreferences =
        LauncherPreferences(accentMode = AccentMode.fromPersistedValue(value))

    fun encodeAccent(preferences: LauncherPreferences): String =
        preferences.accentMode.persistedValue

    fun resetAppearance(): LauncherPreferences = LauncherPreferences()
}

data class LauncherAccessState(
    val isDefaultHome: Boolean = false,
    val continuityGranted: Boolean = false,
    val calendarGranted: Boolean = false,
    val approximateLocationGranted: Boolean = false,
    val audioVisualizerGranted: Boolean = false,
    val focusNotificationsGranted: Boolean = false,
    val exactAlarmsGranted: Boolean = false,
)

enum class LauncherSurface { HOME, EVERYTHING, SETTINGS }

enum class SettingsOrigin { HOME, EVERYTHING }

data class LauncherNavigationState(
    val surface: LauncherSurface = LauncherSurface.HOME,
    val settingsOrigin: SettingsOrigin = SettingsOrigin.HOME,
) {
    fun openEverything() = copy(surface = LauncherSurface.EVERYTHING)

    fun closeToHome() = copy(surface = LauncherSurface.HOME)

    fun openSettings(): LauncherNavigationState = copy(
        surface = LauncherSurface.SETTINGS,
        settingsOrigin = if (surface == LauncherSurface.EVERYTHING) {
            SettingsOrigin.EVERYTHING
        } else {
            SettingsOrigin.HOME
        },
    )

    fun closeSettings(): LauncherNavigationState = copy(
        surface = when (settingsOrigin) {
            SettingsOrigin.HOME -> LauncherSurface.HOME
            SettingsOrigin.EVERYTHING -> LauncherSurface.EVERYTHING
        },
    )

    fun handleHomePressed(): LauncherNavigationState = when (surface) {
        LauncherSurface.HOME -> openEverything()
        LauncherSurface.EVERYTHING,
        LauncherSurface.SETTINGS,
        -> closeToHome()
    }
}
