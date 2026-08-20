package dev.vicent.veil.config

import dev.vicent.veil.launcher.model.HomeButtonActionSpec
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.QuickActionSpec
import dev.vicent.veil.launcher.model.WorkspaceCapability
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LauncherConfigTest {
    @Test
    fun `every context defines five stable slots`() {
        LauncherConfig.workspaceCatalog.forEach { context ->
            assertEquals(LauncherConfig.QUICK_ACTION_COUNT, context.quickActions.size)
        }
    }

    @Test
    fun `catalog has one fixed home and six selectable views with stable identities`() {
        val catalog = LauncherConfig.workspaceCatalog

        assertEquals(7, catalog.size)
        assertEquals(catalog.size, catalog.map { it.id }.distinct().size)
        assertEquals(catalog.size, catalog.map { it.kind }.distinct().size)
        assertEquals(LauncherContextKind.entries.toSet(), catalog.map { it.kind }.toSet())
        assertEquals(1, catalog.count { it.kind == LauncherContextKind.CURRENT })
        assertTrue(catalog.all { it.titleResource != 0 && it.descriptionResource != 0 })
    }

    @Test
    fun `each view declares the capabilities that activate its data`() {
        val byKind = LauncherConfig.workspaceCatalog.associateBy { it.kind }

        assertTrue(WorkspaceCapability.CALENDAR in byKind.getValue(LauncherContextKind.WORK).capabilities)
        assertTrue(WorkspaceCapability.CALENDAR in byKind.getValue(LauncherContextKind.FOCUS).capabilities)
        assertTrue(WorkspaceCapability.AUDIO in byKind.getValue(LauncherContextKind.MEDIA).capabilities)
        assertTrue(WorkspaceCapability.STEAM in byKind.getValue(LauncherContextKind.GAME).capabilities)
        assertTrue(WorkspaceCapability.SYSTEM_STATUS in byKind.getValue(LauncherContextKind.TOOLS).capabilities)
        assertTrue(WorkspaceCapability.WEATHER in byKind.getValue(LauncherContextKind.ON_THE_GO).capabilities)
    }

    @Test
    fun `game defaults are automatic instead of naming personal games`() {
        val gameSlots = LauncherConfig.workspaceCatalog
            .single { it.kind == LauncherContextKind.GAME }
            .quickActions
            .filterIsInstance<QuickActionSpec.App>()

        assertTrue(gameSlots.all { it.packageCandidates.isEmpty() })
    }

    @Test
    fun `semantic defaults offer provider alternatives`() {
        val currentSlots = LauncherConfig.workspaceCatalog
            .single { it.kind == LauncherContextKind.CURRENT }
            .quickActions
            .filterIsInstance<QuickActionSpec.App>()

        assertTrue(currentSlots.all { it.packageCandidates.size >= 3 })
        val homeCamera = LauncherConfig.homeButton.onLongPress as HomeButtonActionSpec.App
        assertTrue(homeCamera.packageCandidates.size >= 3)
    }
}
