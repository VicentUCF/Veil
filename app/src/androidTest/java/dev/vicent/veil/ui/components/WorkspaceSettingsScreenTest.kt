package dev.vicent.veil.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.vicent.veil.config.LauncherConfig
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.ui.theme.VeilTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WorkspaceSettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun recommendedSelectionIsVisibleOnFirstRun() {
        showScreen(firstRun = true)

        composeRule.onNodeWithText("> monta tu Veil").assertIsDisplayed()
        composeRule.onNodeWithText("Planificación").assertIsDisplayed()
        composeRule.onNodeWithText("Concentración").assertIsDisplayed()
        composeRule.onNodeWithText("Media").assertIsDisplayed()
        composeRule.onNodeWithText("Dispositivo").assertIsDisplayed()
    }

    @Test
    fun replacementPickerOnlyOffersUnselectedViews() {
        var replacement: Pair<Int, LauncherContextKind>? = null
        showScreen(onWorkspaceReplaced = { position, kind -> replacement = position to kind })

        composeRule.onNodeWithText("Planificación").performClick()
        composeRule.onNodeWithText("Juegos").performClick()

        composeRule.runOnIdle {
            assertEquals(0 to LauncherContextKind.GAME, replacement)
        }
    }

    private fun showScreen(
        firstRun: Boolean = false,
        onWorkspaceReplaced: (Int, LauncherContextKind) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            VeilTheme(palette = LauncherConfig.palette) {
                WorkspaceSettingsScreen(
                    preferences = LauncherPreferences(),
                    catalog = LauncherConfig.workspaceCatalog.filter {
                        it.kind != LauncherContextKind.CURRENT
                    },
                    firstRun = firstRun,
                    onBack = {},
                    onWorkspaceReplaced = onWorkspaceReplaced,
                    onWorkspaceMoved = { _, _ -> },
                    onComplete = {},
                )
            }
        }
    }
}
