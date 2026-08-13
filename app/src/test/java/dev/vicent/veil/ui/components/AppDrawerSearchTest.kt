package dev.vicent.veil.ui.components

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AppDrawerSearchTest {
    @Test
    fun `Veil settings is visible without a query and for relevant terms`() {
        assertTrue(veilSettingsMatches(""))
        assertTrue(veilSettingsMatches("ajustes veil"))
        assertTrue(veilSettingsMatches("color acento"))
        assertTrue(veilSettingsMatches("permisos launcher"))
    }

    @Test
    fun `Veil settings does not match unrelated searches`() {
        assertFalse(veilSettingsMatches("calculadora"))
    }
}
