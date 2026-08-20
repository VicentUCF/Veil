package dev.vicent.veil.config

import androidx.compose.ui.graphics.Color
import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.ui.theme.VeilPalette

data class AccentPreset(
    val mode: AccentMode,
    val color: Color,
)

object AccentPalette {
    val presets = listOf(
        AccentPreset(AccentMode.VEIL, Color(0xFFF09B8D)),
        AccentPreset(AccentMode.AMBER, Color(0xFFE4B96A)),
        AccentPreset(AccentMode.SAGE, Color(0xFF91C69A)),
        AccentPreset(AccentMode.SKY, Color(0xFF83B9E6)),
        AccentPreset(AccentMode.LILAC, Color(0xFFBAA5E5)),
    )

    fun resolveColor(
        mode: AccentMode,
        systemAccent: Color?,
    ): Color = when (mode) {
        AccentMode.SYSTEM -> systemAccent ?: presets.first().color
        else -> presets.first { it.mode == mode }.color
    }

    fun resolvePalette(
        base: VeilPalette,
        mode: AccentMode,
        systemAccent: Color?,
    ): VeilPalette = base.copy(accentActive = resolveColor(mode, systemAccent))
}
