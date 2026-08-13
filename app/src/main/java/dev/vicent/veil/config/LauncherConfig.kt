package dev.vicent.veil.config

import androidx.compose.ui.graphics.Color
import dev.vicent.veil.launcher.model.LauncherContext
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.QuickActionSpec
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

    const val quickActionCount = 5

    private fun apps(vararg packageNames: String) =
        packageNames.map(QuickActionSpec::App)

    val contexts = listOf(
        LauncherContext(
            id = "home",
            label = "CURRENT",
            kind = LauncherContextKind.CURRENT,
            quickActions = apps(
                "com.google.android.dialer",
                "com.google.android.apps.messaging",
                "com.brave.browser",
                "com.android.camera",
                "com.whatsapp",
            ),
        ),
        LauncherContext(
            id = "work",
            label = "WORK",
            kind = LauncherContextKind.WORK,
            quickActions = apps(
                "com.github.android",
                "com.microsoft.office.outlook",
                "com.microsoft.teams",
                "notion.id",
                "com.termux",
            ),
        ),
        LauncherContext(
            id = "media",
            label = "MEDIA",
            kind = LauncherContextKind.MEDIA,
            quickActions = apps(
                "com.spotify.music",
                "com.google.android.youtube",
                "tv.twitch.android.app",
                "com.netflix.mediaclient",
                "com.google.android.apps.photos",
            ),
        ),
        LauncherContext(
            id = "social",
            label = "SOCIAL",
            kind = LauncherContextKind.SOCIAL,
            quickActions = apps(
                "com.whatsapp",
                "org.telegram.messenger",
                "com.discord",
                "com.instagram.android",
                "com.reddit.frontpage",
            ),
        ),
        LauncherContext(
            id = "tools",
            label = "TOOLS",
            kind = LauncherContextKind.TOOLS,
            quickActions = apps(
                "com.android.settings",
                "com.mi.android.globalFileexplorer",
                "com.miui.calculator",
                "com.android.deskclock",
                "com.x8bit.bitwarden",
            ),
        ),
    )
}
