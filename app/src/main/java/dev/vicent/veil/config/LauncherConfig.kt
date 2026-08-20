package dev.vicent.veil.config

import androidx.compose.ui.graphics.Color
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.LauncherContext
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.HomeButtonActionSpec
import dev.vicent.veil.launcher.model.HomeButtonConfig
import dev.vicent.veil.launcher.model.QuickActionSpec
import dev.vicent.veil.launcher.model.WorkspaceCapability
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
        tileBackground = Color(0xFF101418),
        dialogBackground = Color(0xFF111518),
        quickButtonBackground = Color(0xFF0C1013),
        subtleFill = Color.White.copy(alpha = 0.055f),
        indicatorOutline = Color(0xFF101418),
        divider = Color(0x387D858A),
        error = Color(0xFFD96D6D),
        success = Color(0xFF80A884),
    )

    const val QUICK_ACTION_COUNT = 5

    private val CAMERA_PACKAGES = listOf(
        "com.google.android.GoogleCamera",
        "com.android.camera2",
        "com.android.camera",
        "com.sec.android.app.camera",
    )

    /** CURRENT action button: a tap opens Everything; holding it opens the camera. */
    val homeButton = HomeButtonConfig(
        onTap = HomeButtonActionSpec.Everything,
        onLongPress = HomeButtonActionSpec.App(CAMERA_PACKAGES),
    )

    private fun appSlot(vararg packageCandidates: String) =
        QuickActionSpec.App(packageCandidates.toList())

    private fun automaticSlots() = List(QUICK_ACTION_COUNT) { QuickActionSpec.App(emptyList()) }

    val workspaceCatalog = listOf(
        LauncherContext(
            id = "home",
            kind = LauncherContextKind.CURRENT,
            titleResource = R.string.workspace_current_title,
            descriptionResource = R.string.workspace_current_description,
            capabilities = setOf(
                WorkspaceCapability.WEATHER,
                WorkspaceCapability.CONTINUITY,
                WorkspaceCapability.SYSTEM_STATUS,
            ),
            quickActions = listOf(
                appSlot(
                    "com.google.android.dialer",
                    "com.android.dialer",
                    "com.samsung.android.dialer",
                ),
                appSlot(
                    "com.google.android.apps.messaging",
                    "com.android.messaging",
                    "com.samsung.android.messaging",
                ),
                appSlot(
                    "com.android.chrome",
                    "org.mozilla.firefox",
                    "com.brave.browser",
                    "com.microsoft.emmx",
                ),
                appSlot(*CAMERA_PACKAGES.toTypedArray()),
                appSlot(
                    "com.google.android.apps.youtube.music",
                    "com.spotify.music",
                    "com.apple.android.music",
                    "deezer.android.app",
                ),
            ),
        ),
        LauncherContext(
            id = "planning",
            kind = LauncherContextKind.WORK,
            titleResource = R.string.workspace_planning_title,
            descriptionResource = R.string.workspace_planning_description,
            capabilities = setOf(
                WorkspaceCapability.CALENDAR,
                WorkspaceCapability.CONTINUITY,
                WorkspaceCapability.WORK_PROGRESS,
            ),
            quickActions = listOf(
                appSlot("com.github.android", "com.gitlab.android"),
                appSlot(
                    "com.google.android.gm",
                    "com.microsoft.office.outlook",
                    "ch.protonmail.android",
                ),
                appSlot("com.Slack", "com.microsoft.teams", "com.google.android.apps.tachyon"),
                appSlot("com.google.android.keep", "notion.id", "md.obsidian"),
                appSlot(
                    "com.google.android.apps.authenticator2",
                    "com.azure.authenticator",
                    "com.beemdevelopment.aegis",
                ),
            ),
        ),
        LauncherContext(
            id = "focus",
            kind = LauncherContextKind.FOCUS,
            titleResource = R.string.workspace_focus_title,
            descriptionResource = R.string.workspace_focus_description,
            capabilities = setOf(WorkspaceCapability.CALENDAR),
            quickActions = listOf(
                appSlot("com.google.android.calendar", "com.microsoft.office.outlook"),
                appSlot("com.google.android.keep", "notion.id", "md.obsidian"),
                appSlot("com.google.android.deskclock", "com.android.deskclock"),
                appSlot("com.google.android.gm", "ch.protonmail.android"),
                appSlot("com.google.android.apps.authenticator2", "com.beemdevelopment.aegis"),
            ),
        ),
        LauncherContext(
            id = "media",
            kind = LauncherContextKind.MEDIA,
            titleResource = R.string.workspace_media_title,
            descriptionResource = R.string.workspace_media_description,
            capabilities = setOf(
                WorkspaceCapability.CONTINUITY,
                WorkspaceCapability.AUDIO,
            ),
            quickActions = listOf(
                appSlot(
                    "com.google.android.apps.youtube.music",
                    "com.spotify.music",
                    "com.apple.android.music",
                    "deezer.android.app",
                ),
                appSlot("com.google.android.youtube", "org.videolan.vlc", "com.mxtech.videoplayer.ad"),
                appSlot("tv.twitch.android.app", "com.google.android.apps.youtube.gaming"),
                appSlot(
                    "com.netflix.mediaclient",
                    "com.amazon.avod.thirdpartyclient",
                    "com.disney.disneyplus",
                ),
                appSlot(
                    "com.google.android.apps.photos",
                    "com.sec.android.gallery3d",
                    "com.miui.gallery",
                ),
            ),
        ),
        LauncherContext(
            id = "game",
            kind = LauncherContextKind.GAME,
            titleResource = R.string.workspace_game_title,
            descriptionResource = R.string.workspace_game_description,
            capabilities = setOf(WorkspaceCapability.STEAM),
            quickActions = automaticSlots(),
        ),
        LauncherContext(
            id = "device",
            kind = LauncherContextKind.TOOLS,
            titleResource = R.string.workspace_device_title,
            descriptionResource = R.string.workspace_device_description,
            capabilities = setOf(WorkspaceCapability.SYSTEM_STATUS),
            quickActions = listOf(
                appSlot("com.android.settings"),
                appSlot(
                    "com.google.android.apps.nbu.files",
                    "com.sec.android.app.myfiles",
                    "com.mi.android.globalFileexplorer",
                    "com.android.documentsui",
                ),
                appSlot(
                    "com.google.android.calculator",
                    "com.android.calculator2",
                    "com.sec.android.app.popupcalculator",
                    "com.miui.calculator",
                ),
                appSlot(
                    "com.google.android.deskclock",
                    "com.android.deskclock",
                    "com.sec.android.app.clockpackage",
                ),
                appSlot(
                    "com.x8bit.bitwarden",
                    "com.onepassword.android",
                    "proton.android.pass",
                    "com.kunzisoft.keepass.free",
                ),
            ),
        ),
        LauncherContext(
            id = "on_the_go",
            kind = LauncherContextKind.ON_THE_GO,
            titleResource = R.string.workspace_on_the_go_title,
            descriptionResource = R.string.workspace_on_the_go_description,
            capabilities = setOf(
                WorkspaceCapability.CALENDAR,
                WorkspaceCapability.WEATHER,
                WorkspaceCapability.CONTINUITY,
            ),
            quickActions = listOf(
                appSlot("com.google.android.apps.maps", "com.waze", "com.here.app.maps"),
                appSlot("com.google.android.calendar", "com.microsoft.office.outlook"),
                appSlot(*CAMERA_PACKAGES.toTypedArray()),
                appSlot("com.google.android.apps.walletnfcrel", "com.samsung.android.spay"),
                appSlot("com.google.android.apps.translate", "com.microsoft.translator"),
            ),
        ),
    )
}
