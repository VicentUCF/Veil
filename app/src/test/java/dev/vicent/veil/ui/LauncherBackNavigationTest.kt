package dev.vicent.veil.ui

import dev.vicent.veil.launcher.model.LauncherSurface
import kotlin.test.Test
import kotlin.test.assertEquals

class LauncherBackNavigationTest {
    @Test
    fun `Back stays on Home instead of finishing the launcher`() {
        assertEquals(
            LauncherBackAction.KEEP_HOME,
            launcherBackAction(
                surface = LauncherSurface.HOME,
                isSettingsDetailOpen = false,
            ),
        )
    }

    @Test
    fun `Back closes Everything`() {
        assertEquals(
            LauncherBackAction.CLOSE_EVERYTHING,
            launcherBackAction(
                surface = LauncherSurface.EVERYTHING,
                isSettingsDetailOpen = false,
            ),
        )
    }

    @Test
    fun `Back closes the settings detail before settings`() {
        assertEquals(
            LauncherBackAction.CLOSE_SETTINGS_DETAIL,
            launcherBackAction(
                surface = LauncherSurface.SETTINGS,
                isSettingsDetailOpen = true,
            ),
        )
        assertEquals(
            LauncherBackAction.CLOSE_SETTINGS,
            launcherBackAction(
                surface = LauncherSurface.SETTINGS,
                isSettingsDetailOpen = false,
            ),
        )
    }
}
