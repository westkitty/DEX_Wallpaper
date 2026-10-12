package com.westkitty.dexwallpaper

import androidx.compose.ui.test.*
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.westkitty.dexwallpaper.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import java.io.File
import java.util.UUID
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
    @Test fun importedArtworkRemainsDrawableAcrossLibraryEditorRecreation() {
        // Regression for the observed Galaxy Tab S9 Ultra crash: a Compose painter
        // must never be handed an Android Bitmap recycled during disposal.
        val app = compose.activity.application as DexApplication
        val source = File(compose.activity.cacheDir, "compose-bitmap-${UUID.randomUUID()}.png")
        val generated = Bitmap.createBitmap(96, 64, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.MAGENTA)
        }
        try {
            source.outputStream().use { assert(generated.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            runBlocking {
                app.graph.repository.initialize()
                val result = app.graph.importer.importUris(listOf(Uri.fromFile(source)), "default") {}
                assertEquals("A valid local PNG should import without rejection", 0, result.rejected)
                assertEquals(1, result.imported)
            }
            val compact = compose.onAllNodesWithContentDescription("Choose workspace")
                .fetchSemanticsNodes().isNotEmpty()
            if (compact) {
                compose.onNodeWithContentDescription("Choose workspace").performClick()
                compose.onNodeWithText("Artwork library").performClick()
            } else {
                compose.onNodeWithText("    Library").performClick()
            }
            compose.waitUntil(10000) {
                compose.onAllNodesWithText(source.name).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onAllNodesWithText(source.name).onFirst().performClick()
            compose.waitUntil(10000) {
                compose.onAllNodesWithContentDescription("Preview of ${source.name}")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Edit").performClick()
            compose.waitUntil(10000) {
                compose.onAllNodesWithText("FrameSense editor").fetchSemanticsNodes().isNotEmpty()
            }
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText("FrameSense editor").assertIsDisplayed()
            compose.waitForIdle()
        } finally {
            generated.recycle() // Test fixture is never passed to a Compose painter.
            source.delete() // The imported copy stays within the isolated test app.
        }
    }

}
