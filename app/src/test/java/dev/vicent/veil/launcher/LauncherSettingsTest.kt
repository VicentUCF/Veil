package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.launcher.model.LauncherNavigationState
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.LauncherPreferencesPolicy
import dev.vicent.veil.launcher.model.ContextAppPreferencesPolicy
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.LauncherSurface
import dev.vicent.veil.launcher.model.HomeTextTone
import dev.vicent.veil.launcher.model.HomeTextWeight
import kotlin.test.assertEquals
import org.junit.Test

class LauncherSettingsTest {
    @Test
    fun `preference codec round trips every accent`() {
        AccentMode.entries.forEach { mode ->
            val encoded = LauncherPreferencesPolicy.encodeAccent(
                LauncherPreferences(accentMode = mode),
            )
            assertEquals(mode, LauncherPreferencesPolicy.decodeAppearance(encoded).accentMode)
        }
    }

    @Test
    fun `missing or unknown accent falls back to Veil`() {
        assertEquals(AccentMode.VEIL, LauncherPreferencesPolicy.decodeAppearance(null).accentMode)
        assertEquals(
            AccentMode.VEIL,
            LauncherPreferencesPolicy.decodeAppearance("future").accentMode,
        )
        val customized = LauncherPreferences(
            accentMode = AccentMode.SKY,
            homeTextTone = HomeTextTone.DARK,
            homeTextWeight = HomeTextWeight.SEMIBOLD,
            wallpaperScrimEnabled = false,
            musicProviderPackage = "com.spotify.music",
            contextAppOverrides = mapOf(LauncherContextKind.WORK to listOf("com.example")),
        )
        val reset = LauncherPreferencesPolicy.resetAppearance(customized)
        assertEquals(AccentMode.VEIL, reset.accentMode)
        assertEquals(HomeTextTone.LIGHT, reset.homeTextTone)
        assertEquals(HomeTextWeight.LIGHT, reset.homeTextWeight)
        assertEquals(true, reset.wallpaperScrimEnabled)
        assertEquals(customized.musicProviderPackage, reset.musicProviderPackage)
        assertEquals(customized.contextAppOverrides, reset.contextAppOverrides)
    }

    @Test
    fun `home text appearance decodes persisted values and falls back safely`() {
        val decoded = LauncherPreferencesPolicy.decodeAppearance(
            accent = AccentMode.AMBER.persistedValue,
            homeTextTone = HomeTextTone.DARK.persistedValue,
            homeTextWeight = HomeTextWeight.REGULAR.persistedValue,
            wallpaperScrimEnabled = false,
        )

        assertEquals(HomeTextTone.DARK, decoded.homeTextTone)
        assertEquals(HomeTextWeight.REGULAR, decoded.homeTextWeight)
        assertEquals(false, decoded.wallpaperScrimEnabled)
        assertEquals(
            HomeTextTone.LIGHT,
            LauncherPreferencesPolicy.decodeAppearance(null, "future", null).homeTextTone,
        )
        assertEquals(
            HomeTextWeight.LIGHT,
            LauncherPreferencesPolicy.decodeAppearance(null, null, "future").homeTextWeight,
        )
    }

    @Test
    fun `clearing one context slot preserves position and other apps`() {
        val slots = listOf("one", "teams", "three", "four", "five")

        assertEquals(
            listOf("one", null, "three", "four", "five"),
            ContextAppPreferencesPolicy.update(slots, 1, null),
        )
    }

    @Test
    fun `replacing one context slot does not reorder the others`() {
        val slots = listOf("one", null, "three", "four", "five")

        assertEquals(
            listOf("one", "new", "three", "four", "five"),
            ContextAppPreferencesPolicy.update(slots, 1, "new"),
        )
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
