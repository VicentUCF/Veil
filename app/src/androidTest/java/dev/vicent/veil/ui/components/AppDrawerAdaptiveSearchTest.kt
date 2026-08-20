package dev.vicent.veil.ui.components

import android.content.ComponentName
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import dev.vicent.veil.config.LauncherConfig
import dev.vicent.veil.launcher.model.AppSearchLearningEntry
import dev.vicent.veil.launcher.model.AppSearchLearningState
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.ui.theme.VeilTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppDrawerAdaptiveSearchTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun searchStartsFocusedFindsTypoAndKeepsPackageHidden() {
        showDrawer()

        composeRule.onNodeWithTag(APP_DRAWER_SEARCH_TEST_TAG)
            .assertIsFocused()
            .performTextInput("instgram")

        composeRule.onNodeWithText("Instagram").assertIsDisplayed()
        composeRule.onAllNodesWithText("com.instagram.android").assertCountEquals(0)
        composeRule.onAllNodesWithText("Calendar").assertCountEquals(0)
    }

    @Test
    fun imeLaunchesFirstAdaptiveResultWithTheCurrentQuery() {
        var selected: Pair<String, String>? = null
        val now = System.currentTimeMillis()
        showDrawer(
            learning = AppSearchLearningState(
                listOf(
                    AppSearchLearningEntry(
                        query = "inst",
                        packageName = "com.instagram.android",
                        selectionCount = 3,
                        lastSelectedAtMillis = now,
                    ),
                ),
            ),
            onSelected = { app, query -> selected = app.packageName to query },
        )

        val search = composeRule.onNodeWithTag(APP_DRAWER_SEARCH_TEST_TAG)
        search.performTextInput("inst")
        search.performImeAction()

        composeRule.runOnIdle {
            assertEquals("com.instagram.android" to "inst", selected)
        }
    }

    @Test
    fun removingAndRecreatingEverythingClearsTheQuery() {
        var visible by mutableStateOf(true)
        composeRule.setContent {
            if (visible) DrawerContent()
        }
        composeRule.onNodeWithTag(APP_DRAWER_SEARCH_TEST_TAG).performTextInput("inst")

        composeRule.runOnIdle { visible = false }
        composeRule.waitForIdle()
        composeRule.runOnIdle { visible = true }

        composeRule.onNodeWithTag(APP_DRAWER_SEARCH_TEST_TAG).assertTextEquals("")
    }

    @Test
    fun longPressKeepsAndForwardsTheCurrentQuery() {
        var longPressedQuery: String? = null
        composeRule.setContent {
            VeilTheme(palette = LauncherConfig.palette) {
                AppDrawer(
                    installedApps = apps,
                    searchLearning = AppSearchLearningState(),
                    settingsShortcuts = emptyList(),
                    isLoading = false,
                    isOpen = true,
                    onAppSelected = { _, _ -> },
                    onAppLongPressed = { _, query -> longPressedQuery = query },
                    onSettingsSelected = {},
                    onVeilSettingsSelected = {},
                    continuityAccessGranted = false,
                    onContinuityAccessSelected = {},
                    onClose = {},
                )
            }
        }
        composeRule.onNodeWithTag(APP_DRAWER_SEARCH_TEST_TAG).performTextInput("inst")

        composeRule.onNodeWithText("Instagram").performTouchInput { longClick() }

        composeRule.runOnIdle { assertEquals("inst", longPressedQuery) }
        composeRule.onNodeWithTag(APP_DRAWER_SEARCH_TEST_TAG).assertTextEquals("inst")
    }

    private fun showDrawer(
        learning: AppSearchLearningState = AppSearchLearningState(),
        onSelected: (LauncherApp, String) -> Unit = { _, _ -> },
    ) {
        composeRule.setContent {
            DrawerContent(learning, onSelected)
        }
    }

    @androidx.compose.runtime.Composable
    private fun DrawerContent(
        learning: AppSearchLearningState = AppSearchLearningState(),
        onSelected: (LauncherApp, String) -> Unit = { _, _ -> },
    ) {
        VeilTheme(palette = LauncherConfig.palette) {
            AppDrawer(
                installedApps = apps,
                searchLearning = learning,
                settingsShortcuts = emptyList(),
                isLoading = false,
                isOpen = true,
                onAppSelected = onSelected,
                onAppLongPressed = { _, _ -> },
                onSettingsSelected = {},
                onVeilSettingsSelected = {},
                continuityAccessGranted = false,
                onContinuityAccessSelected = {},
                onClose = {},
            )
        }
    }

    private val apps = listOf(
        LauncherApp(
            packageName = "com.example.instant",
            label = "Instant Notes",
            componentName = ComponentName("com.example.instant", "MainActivity"),
        ),
        LauncherApp(
            packageName = "com.instagram.android",
            label = "Instagram",
            componentName = ComponentName("com.instagram.android", "MainActivity"),
        ),
        LauncherApp(
            packageName = "com.example.calendar",
            label = "Calendar",
            componentName = ComponentName("com.example.calendar", "MainActivity"),
        ),
    )
}
