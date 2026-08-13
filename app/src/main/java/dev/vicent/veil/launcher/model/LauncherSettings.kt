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

enum class HomeTextTone(val persistedValue: String) {
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromPersistedValue(value: String?): HomeTextTone =
            entries.firstOrNull { it.persistedValue == value } ?: LIGHT
    }
}

enum class HomeTextWeight(val persistedValue: String) {
    LIGHT("light"),
    REGULAR("regular"),
    SEMIBOLD("semibold");

    companion object {
        fun fromPersistedValue(value: String?): HomeTextWeight =
            entries.firstOrNull { it.persistedValue == value } ?: LIGHT
    }
}

data class LauncherPreferences(
    val accentMode: AccentMode = AccentMode.VEIL,
    val homeTextTone: HomeTextTone = HomeTextTone.LIGHT,
    val homeTextWeight: HomeTextWeight = HomeTextWeight.LIGHT,
    val wallpaperScrimEnabled: Boolean = true,
    val musicProviderPackage: String? = null,
    val contextAppOverrides: Map<LauncherContextKind, List<String?>> = emptyMap(),
)

object LauncherPreferencesPolicy {
    fun decodeAppearance(
        accent: String?,
        homeTextTone: String? = null,
        homeTextWeight: String? = null,
        wallpaperScrimEnabled: Boolean = true,
    ): LauncherPreferences = LauncherPreferences(
        accentMode = AccentMode.fromPersistedValue(accent),
        homeTextTone = HomeTextTone.fromPersistedValue(homeTextTone),
        homeTextWeight = HomeTextWeight.fromPersistedValue(homeTextWeight),
        wallpaperScrimEnabled = wallpaperScrimEnabled,
    )

    fun encodeAccent(preferences: LauncherPreferences): String =
        preferences.accentMode.persistedValue

    fun resetAppearance(current: LauncherPreferences): LauncherPreferences =
        current.copy(
            accentMode = AccentMode.VEIL,
            homeTextTone = HomeTextTone.LIGHT,
            homeTextWeight = HomeTextWeight.LIGHT,
            wallpaperScrimEnabled = true,
        )
}

object ContextAppPreferencesPolicy {
    fun normalize(slots: List<String?>, count: Int = 5): List<String?> =
        (slots.take(count) + List(count) { null }).take(count)

    fun update(
        currentSlots: List<String?>,
        slotIndex: Int,
        packageName: String?,
        count: Int = 5,
    ): List<String?> {
        if (slotIndex !in 0 until count) return normalize(currentSlots, count)
        return normalize(currentSlots, count).toMutableList().apply {
            this[slotIndex] = packageName
        }
    }
}

sealed interface SettingsAppTarget {
    data object MusicProvider : SettingsAppTarget
    data class ContextSlot(
        val kind: LauncherContextKind,
        val slotIndex: Int,
    ) : SettingsAppTarget
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
