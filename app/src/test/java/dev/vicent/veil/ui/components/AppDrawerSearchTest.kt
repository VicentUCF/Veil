package dev.vicent.veil.ui.components

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AppDrawerSearchTest {
    private val searchTerms =
        "ajustes configuracion preferencias veil tema color acento fondo wallpaper " +
            "permisos privacidad launcher"

    @Test
    fun `Veil settings is visible without a query and for relevant terms`() {
        assertTrue(veilSettingsMatches("", searchTerms))
        assertTrue(veilSettingsMatches("ajustes veil", searchTerms))
        assertTrue(veilSettingsMatches("color acento", searchTerms))
        assertTrue(veilSettingsMatches("permisos launcher", searchTerms))
    }

    @Test
    fun `Veil settings does not match unrelated searches`() {
        assertFalse(veilSettingsMatches("calculadora", searchTerms))
    }
}
