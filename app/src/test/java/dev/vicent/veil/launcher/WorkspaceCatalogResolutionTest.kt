package dev.vicent.veil.launcher

import dev.vicent.veil.config.LauncherConfig
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.LauncherPreferences
import org.junit.Test
import kotlin.test.assertEquals

class WorkspaceCatalogResolutionTest {
    @Test
    fun `resolver keeps home fixed and follows the four persisted positions`() {
        val selection = listOf(
            LauncherContextKind.ON_THE_GO,
            LauncherContextKind.GAME,
            LauncherContextKind.FOCUS,
            LauncherContextKind.MEDIA,
        )
        val resolver = LauncherContextResolver(
            workspaceCatalog = LauncherConfig.workspaceCatalog,
            quickActionCount = LauncherConfig.QUICK_ACTION_COUNT,
        )

        val resolved = resolver.resolve(
            installedApps = emptyList(),
            preferences = LauncherPreferences(selectedWorkspaceKinds = selection),
            appScanComplete = true,
        )

        assertEquals(
            listOf(LauncherContextKind.CURRENT) + selection,
            resolved.map { it.definition.kind },
        )
        assertEquals(List(5) { 5 }, resolved.map { it.quickActions.size })
    }
}
