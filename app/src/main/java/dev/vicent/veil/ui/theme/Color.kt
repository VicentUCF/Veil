package dev.vicent.veil.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class VeilPalette(
    val contentPrimary: Color,
    val contentSecondary: Color,
    val contentMuted: Color,
    val accentActive: Color,
    val barBackground: Color,
    val drawerBackground: Color,
    val fieldBackground: Color,
    val divider: Color,
    val error: Color,
    val success: Color,
)
