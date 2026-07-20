package com.vamshi.field.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.vamshi.field.ui.dashboard.DashboardContent
import com.vamshi.field.ui.dashboard.DashboardUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class ResponsiveGridTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun gridAdaptsToTabletWidth() {
        composeTestRule.setContent {
            Box(modifier = Modifier.width(800.dp)) {
                DashboardContent(
                    uiState = DashboardUiState(),
                    onAction = {}
                )
            }
        }

        // In a wide layout (800.dp), QuickActionCards should have enough space to be placed side-by-side.
        val quickTestBounds = composeTestRule.onNodeWithText("Quick Test").getUnclippedBoundsInRoot()
        val rosterBounds = composeTestRule.onNodeWithText("Roster").getUnclippedBoundsInRoot()

        // They should be on the same horizontal line (same top)
        assertEquals(quickTestBounds.top, rosterBounds.top)
        
        // And different left positions
        assertNotEquals(quickTestBounds.left, rosterBounds.left)
    }

    @Test
    fun gridAdaptsToPhoneWidth() {
        composeTestRule.setContent {
            Box(modifier = Modifier.width(200.dp)) {
                DashboardContent(
                    uiState = DashboardUiState(),
                    onAction = {}
                )
            }
        }

        // In a narrow layout (200.dp), QuickActionCards should be stacked vertically.
        val quickTestBounds = composeTestRule.onNodeWithText("Quick Test").getUnclippedBoundsInRoot()
        val rosterBounds = composeTestRule.onNodeWithText("Roster").getUnclippedBoundsInRoot()

        // They should be on different horizontal lines (different top)
        assertNotEquals(quickTestBounds.top, rosterBounds.top)
        
        // And same left positions
        assertEquals(quickTestBounds.left, rosterBounds.left)
    }
}
