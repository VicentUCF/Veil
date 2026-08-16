package dev.vicent.veil.config

import dev.vicent.veil.launcher.model.HomeButtonActionSpec
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.QuickActionSpec
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LauncherConfigTest {
    @Test
    fun `every context defines five stable slots`() {
        LauncherConfig.contexts.forEach { context ->
            assertEquals(LauncherConfig.QUICK_ACTION_COUNT, context.quickActions.size)
        }
    }

    @Test
    fun `game defaults are automatic instead of naming personal games`() {
        val gameSlots = LauncherConfig.contexts
            .single { it.kind == LauncherContextKind.GAME }
            .quickActions
            .filterIsInstance<QuickActionSpec.App>()

        assertTrue(gameSlots.all { it.packageCandidates.isEmpty() })
    }

    @Test
    fun `semantic defaults offer provider alternatives`() {
        val currentSlots = LauncherConfig.contexts
            .single { it.kind == LauncherContextKind.CURRENT }
            .quickActions
            .filterIsInstance<QuickActionSpec.App>()

        assertTrue(currentSlots.all { it.packageCandidates.size >= 3 })
        val homeCamera = LauncherConfig.homeButton.onLongPress as HomeButtonActionSpec.App
        assertTrue(homeCamera.packageCandidates.size >= 3)
    }
}
