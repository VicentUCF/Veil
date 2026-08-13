package dev.vicent.veil.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val materialColors = if (darkTheme) {
        darkColorScheme(
            primary = palette.accentActive,
            onPrimary = Color(0xFF32110D),
            primaryContainer = Color(0xFF713B33),
            onPrimaryContainer = Color(0xFFFFDAD3),
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
    } else {
        lightColorScheme(
            primary = Color(0xFF8D4136),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDAD3),
            onPrimaryContainer = Color(0xFF3A0905),
            secondary = Color(0xFF765651),
            onSecondary = Color.White,
            background = Color(0xFFFFF8F6),
            onBackground = Color(0xFF211A19),
            surface = Color(0xFFFFF8F6),
            onSurface = Color(0xFF211A19),
            surfaceVariant = Color(0xFFF5DDDA),
            onSurfaceVariant = Color(0xFF534341),
            outline = Color(0xFF857370),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
        )
    }
    MaterialTheme(colorScheme = materialColors) {
        CompositionLocalProvider(LocalVeilPalette provides palette, content = content)
    }
}
