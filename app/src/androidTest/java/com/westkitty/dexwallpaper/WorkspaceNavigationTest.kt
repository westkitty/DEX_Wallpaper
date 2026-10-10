package com.westkitty.dexwallpaper

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
class WorkspaceNavigationTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Test fun firstScreenIsWorkingDashboardAndNavigationSurvivesRecreation() {
        compose.onNodeWithText("Your living wall.").assertIsDisplayed()
        val compact=compose.onAllNodesWithContentDescription("Choose workspace").fetchSemanticsNodes().isNotEmpty()
        if(compact) {
            compose.onNodeWithContentDescription("Choose workspace").performClick()
            compose.onNodeWithText("Artwork library").performClick()
        } else compose.onNodeWithText("    Library").performClick()
        compose.onNodeWithText("Artwork library").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Artwork library").assertIsDisplayed()
    }
}
