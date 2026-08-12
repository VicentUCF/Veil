package dev.vicent.veil.ui.theme

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
    CompositionLocalProvider(LocalVeilPalette provides palette, content = content)
}
