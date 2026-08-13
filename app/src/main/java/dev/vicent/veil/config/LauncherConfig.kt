package dev.vicent.veil.config

import androidx.compose.ui.graphics.Color
import dev.vicent.veil.launcher.model.LauncherContext
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.ui.theme.VeilPalette

object LauncherConfig {
    val palette = VeilPalette(
        contentPrimary = Color(0xFFE8E9E7),
        contentSecondary = Color(0xFFBEC2C3),
        contentMuted = Color(0xFF747C81),
        accentActive = Color(0xFFF09B8D),
        barBackground = Color(0x8F171C20),
        drawerBackground = Color(0xF2111518),
        fieldBackground = Color(0xFF20262A),
        divider = Color(0x387D858A),
        error = Color(0xFFD96D6D),
        success = Color(0xFF80A884),
    )

    const val automaticHomeAppCount = 5

    val contexts = listOf(
        LauncherContext(id = "home", label = "CURRENT", kind = LauncherContextKind.CURRENT),
        LauncherContext(id = "work", label = "WORK", kind = LauncherContextKind.WORK),
        LauncherContext(id = "media", label = "MEDIA", kind = LauncherContextKind.MEDIA),
        LauncherContext(id = "social", label = "SOCIAL", kind = LauncherContextKind.SOCIAL),
        LauncherContext(id = "tools", label = "TOOLS", kind = LauncherContextKind.TOOLS),
    )
}
