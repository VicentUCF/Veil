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
            activityGlyphFor("com.whatsapp", "WhatsApp", AppCategory.SOCIAL),
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
}
