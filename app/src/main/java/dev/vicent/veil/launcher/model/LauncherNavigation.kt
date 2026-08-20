package dev.vicent.veil.launcher.model

sealed interface SettingsAppTarget {
    data object MusicProvider : SettingsAppTarget

    data class HomeButton(val gesture: HomeButtonGesture) : SettingsAppTarget

    data class ContextSlot(
        val kind: LauncherContextKind,
        val slotIndex: Int,
    ) : SettingsAppTarget
}

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
