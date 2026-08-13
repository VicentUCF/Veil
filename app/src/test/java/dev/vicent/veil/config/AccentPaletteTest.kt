package dev.vicent.veil.config

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.vicent.veil.launcher.model.AccentMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class AccentPaletteTest {
    @Test
    fun `all fixed accents meet text contrast on the lightest Veil surface`() {
        val surface = Color(0xFF20262A)

        AccentPalette.presets.forEach { preset ->
            assertTrue(
                contrastRatio(preset.color, surface) >= 4.5f,
                "${preset.label} does not meet WCAG AA contrast",
            )
        }
    }

    @Test
    fun `system accent falls back to Veil when dynamic color is unavailable`() {
        assertEquals(
            Color(0xFFF09B8D),
            AccentPalette.resolveColor(AccentMode.SYSTEM, systemAccent = null),
        )
    }

    @Test
    fun `system accent uses the supplied dynamic color`() {
        val dynamic = Color(0xFFABCDEF)
        assertEquals(dynamic, AccentPalette.resolveColor(AccentMode.SYSTEM, dynamic))
    }

    private fun contrastRatio(foreground: Color, background: Color): Float {
        val lighter = maxOf(foreground.luminance(), background.luminance())
        val darker = minOf(foreground.luminance(), background.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
