package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.launcher.model.LauncherNavigationState
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.LauncherPreferencesPolicy
import dev.vicent.veil.launcher.model.LauncherSurface
import kotlin.test.assertEquals
import org.junit.Test

class LauncherSettingsTest {
    @Test
    fun `preference codec round trips every accent`() {
        AccentMode.entries.forEach { mode ->
            val encoded = LauncherPreferencesPolicy.encodeAccent(
                LauncherPreferences(accentMode = mode),
            )
            assertEquals(mode, LauncherPreferencesPolicy.decodeAccent(encoded).accentMode)
        }
    }

    @Test
    fun `missing or unknown accent falls back to Veil`() {
        assertEquals(AccentMode.VEIL, LauncherPreferencesPolicy.decodeAccent(null).accentMode)
        assertEquals(AccentMode.VEIL, LauncherPreferencesPolicy.decodeAccent("future").accentMode)
        assertEquals(AccentMode.VEIL, LauncherPreferencesPolicy.resetAppearance().accentMode)
    }

    @Test
    fun `settings returns to Everything when opened from Everything`() {
        val settings = LauncherNavigationState().openEverything().openSettings()

        assertEquals(LauncherSurface.SETTINGS, settings.surface)
        assertEquals(LauncherSurface.EVERYTHING, settings.closeSettings().surface)
    }

    @Test
    fun `settings returns home when opened from a workspace`() {
        val settings = LauncherNavigationState().openSettings()

        assertEquals(LauncherSurface.HOME, settings.closeSettings().surface)
    }

    @Test
    fun `Home closes settings and next Home opens Everything`() {
        val home = LauncherNavigationState().openSettings().handleHomePressed()

        assertEquals(LauncherSurface.HOME, home.surface)
        assertEquals(LauncherSurface.EVERYTHING, home.handleHomePressed().surface)
    }
}
