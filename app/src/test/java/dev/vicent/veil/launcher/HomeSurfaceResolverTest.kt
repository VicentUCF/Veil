package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.model.LauncherContextKind
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HomeSurfaceResolverTest {
    @Test
    fun `current shows contextual onboarding only before access or dismissal`() {
        val first = resolve(LauncherContextKind.CURRENT, access = false, dismissed = false)
        val dismissed = resolve(LauncherContextKind.CURRENT, access = false, dismissed = true)

        assertEquals(HomeSurfaceMode.Onboarding, first)
        assertEquals(HomeSurfaceMode.Apps(LauncherContextKind.CURRENT), dismissed)
    }

    @Test
    fun `current and media choose their own continuity while tools stay specialized`() {
        val item = media()
        val current = HomeSurfaceResolver.resolve(
            LauncherContextKind.CURRENT, true, false, item, item,
        )
        val media = HomeSurfaceResolver.resolve(
            LauncherContextKind.MEDIA, true, false, null, item,
        )
        val tools = HomeSurfaceResolver.resolve(
            LauncherContextKind.TOOLS, true, false, item, item,
        )

        assertIs<HomeSurfaceMode.Continuity>(current)
        assertIs<HomeSurfaceMode.Continuity>(media)
        assertEquals(HomeSurfaceMode.Tools, tools)
    }

    @Test
    fun `navigation and progress use the single continuity surface only in current`() {
        val navigation = ContinuityItem.Navigation(
            id = "nav",
            packageName = "maps.app",
            appLabel = "Maps",
            title = "Home",
            subtitle = "14 min",
            updatedAtMillis = 1,
            expiresAtMillis = null,
            supportedActions = setOf(ContinuityAction.OPEN),
        )
        val progress = ContinuityItem.Progress(
            id = "progress",
            packageName = "download.app",
            appLabel = "Downloads",
            title = "Downloading",
            subtitle = null,
            updatedAtMillis = 1,
            expiresAtMillis = null,
            supportedActions = emptySet(),
            progress = .4f,
            isComplete = false,
        )

        assertEquals(
            HomeSurfaceMode.Continuity(navigation),
            HomeSurfaceResolver.resolve(
                LauncherContextKind.CURRENT, true, false, navigation, null,
            ),
        )
        assertEquals(
            HomeSurfaceMode.Continuity(progress),
            HomeSurfaceResolver.resolve(
                LauncherContextKind.CURRENT, true, false, progress, null,
            ),
        )
        assertEquals(
            HomeSurfaceMode.Apps(LauncherContextKind.WORK),
            HomeSurfaceResolver.resolve(
                LauncherContextKind.WORK, true, false, navigation, null,
            ),
        )
    }

    @Test
    fun `revoked access and empty contexts resolve to fallback after onboarding dismissal`() {
        assertEquals(
            HomeSurfaceMode.Apps(LauncherContextKind.CURRENT),
            HomeSurfaceResolver.resolve(
                LauncherContextKind.CURRENT, false, true, null, null,
            ),
        )
        assertEquals(
            HomeSurfaceMode.Apps(LauncherContextKind.SOCIAL),
            HomeSurfaceResolver.resolve(
                LauncherContextKind.SOCIAL, false, false, null, null,
            ),
        )
    }

    private fun resolve(kind: LauncherContextKind, access: Boolean, dismissed: Boolean) =
        HomeSurfaceResolver.resolve(kind, access, dismissed, null, null)

    private fun media() = ContinuityItem.Media(
        id = "media",
        packageName = "media.app",
        appLabel = "Media",
        title = "Track",
        subtitle = null,
        updatedAtMillis = 1,
        expiresAtMillis = null,
        supportedActions = setOf(ContinuityAction.OPEN),
        isPlaying = true,
        isVideo = false,
    )
}
