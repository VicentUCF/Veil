package dev.vicent.veil.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val fallbackPalette = VeilPalette(
    contentPrimary = Color.White,
    contentSecondary = Color.LightGray,
    contentMuted = Color.Gray,
    accentActive = Color(0xFFF09B8D),
    barBackground = Color(0x8F171C20),
    drawerBackground = Color(0xF2111518),
    fieldBackground = Color(0xFF20262A),
    divider = Color.DarkGray,
    error = Color.Red,
    success = Color.Green,
)

val LocalVeilPalette = staticCompositionLocalOf { fallbackPalette }

@Composable
fun VeilTheme(
    palette: VeilPalette,
    content: @Composable () -> Unit,
) {
    val materialColors = darkColorScheme(
        primary = palette.accentActive,
        onPrimary = Color(0xFF101418),
        primaryContainer = palette.fieldBackground,
        onPrimaryContainer = palette.accentActive,
        secondary = palette.contentSecondary,
        onSecondary = Color(0xFF252A2D),
        background = Color(0xFF0F1316),
        onBackground = palette.contentPrimary,
        surface = Color(0xFF181D21),
        onSurface = palette.contentPrimary,
        surfaceVariant = palette.fieldBackground,
        onSurfaceVariant = palette.contentSecondary,
        outline = palette.contentMuted,
        error = palette.error,
        onError = Color(0xFF3B0808),
    )
    MaterialTheme(colorScheme = materialColors) {
        CompositionLocalProvider(LocalVeilPalette provides palette, content = content)
    }
}
