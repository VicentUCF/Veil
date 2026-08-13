package dev.vicent.veil.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

private val fallbackPalette = VeilPalette(
    contentPrimary = Color.White,
    contentSecondary = Color.LightGray,
    contentMuted = Color.Gray,
    accentActive = Color(0xFFF09B8D),
    barBackground = Color(0x8F171C20),
    drawerBackground = Color(0xF2111518),
        fieldBackground = Color(0xFF20262A),
        tileBackground = Color(0xFF101418),
        dialogBackground = Color(0xFF111518),
        quickButtonBackground = Color(0xFF0C1013),
        subtleFill = Color.White.copy(alpha = 0.055f),
        indicatorOutline = Color(0xFF101418),
    divider = Color.DarkGray,
    error = Color.Red,
    success = Color.Green,
)

val LocalVeilPalette = staticCompositionLocalOf { fallbackPalette }

@Immutable
data class VeilTypographyFamilies(
    val content: FontFamily,
    val system: FontFamily,
)

val LocalVeilTypography = staticCompositionLocalOf {
    VeilTypographyFamilies(FontFamily.SansSerif, FontFamily.Monospace)
}

@Composable
fun VeilTheme(
    palette: VeilPalette,
    content: @Composable () -> Unit,
) {
    val typography = VeilTypographyFamilies(FontFamily.SansSerif, FontFamily.Monospace)
    val materialColors = darkColorScheme(
        primary = palette.accentActive,
        onPrimary = palette.tileBackground,
        primaryContainer = palette.fieldBackground,
        onPrimaryContainer = palette.accentActive,
        secondary = palette.contentSecondary,
        onSecondary = Color(0xFF252A2D),
        background = palette.drawerBackground,
        onBackground = palette.contentPrimary,
        surface = palette.dialogBackground,
        onSurface = palette.contentPrimary,
        surfaceVariant = palette.fieldBackground,
        onSurfaceVariant = palette.contentSecondary,
        outline = palette.contentMuted,
        error = palette.error,
        onError = Color(0xFF3B0808),
    )
    MaterialTheme(colorScheme = materialColors) {
        CompositionLocalProvider(
            LocalVeilPalette provides palette,
            LocalVeilTypography provides typography,
            content = content,
        )
    }
}
