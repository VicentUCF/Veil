package dev.vicent.veil.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsHeaderTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun defaultHeaderExposesLocalizedTitleAndBackAction() {
        var backSelected = false
        composeRule.setContent {
            SettingsHeader(onBack = { backSelected = true })
        }

        composeRule.onNodeWithText("> ajustes_de_veil").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Volver").performClick()

        composeRule.runOnIdle { assertTrue(backSelected) }
    }
}
