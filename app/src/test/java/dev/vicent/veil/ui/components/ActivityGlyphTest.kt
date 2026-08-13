package dev.vicent.veil.ui.components

import dev.vicent.veil.launcher.model.AppCategory
import org.junit.Test
import kotlin.test.assertEquals

class ActivityGlyphTest {
    @Test
    fun `current home apps resolve to their bespoke glyphs`() {
        val glyphs = listOf(
            activityGlyphFor("com.google.android.dialer", "Teléfono", AppCategory.GENERAL),
            activityGlyphFor("com.google.android.apps.messaging", "Mensajes", AppCategory.GENERAL),
            activityGlyphFor("com.brave.browser", "Brave", AppCategory.GENERAL),
            activityGlyphFor("com.android.camera", "Cámara", AppCategory.GENERAL),
            activityGlyphFor("com.whatsapp", "WhatsApp", AppCategory.GENERAL),
        )

        assertEquals(
            listOf(
                ActivityGlyphKind.PHONE,
                ActivityGlyphKind.MESSAGE,
                ActivityGlyphKind.BRAVE,
                ActivityGlyphKind.CAMERA,
                ActivityGlyphKind.WHATSAPP,
            ),
            glyphs,
        )
    }

    @Test
    fun `game category resolves to the game glyph`() {
        assertEquals(
            ActivityGlyphKind.GAME,
            activityGlyphFor("com.example.game", "Juego", AppCategory.GAME),
        )
    }
}
