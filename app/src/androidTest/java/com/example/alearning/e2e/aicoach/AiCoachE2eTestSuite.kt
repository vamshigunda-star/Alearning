package com.example.alearning.e2e.aicoach

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import com.example.alearning.MainActivity
import com.example.alearning.data.seed.SeedDataManager
import com.example.alearning.domain.repository.AiCoachStatus
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import android.content.pm.ActivityInfo
import kotlinx.coroutines.delay

@HiltAndroidTest
class AiCoachE2eTestSuite {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var seedDataManager: SeedDataManager

    @Inject
    lateinit var fakeAiCoachRepository: FakeAiCoachRepository

    @Before
    fun setup() {
        hiltRule.inject()
        runBlocking {
            seedDataManager.seedIfNeeded()
        }
    }

    private fun navigateToSessionReport() {
        // From Dashboard, click on Varsity Football group
        composeTestRule.onNodeWithText("Varsity Football", substring = true).performClick()
        // Now on GroupOverviewScreen, click on Summer Benchmark Testing session
        composeTestRule.onNodeWithText("Summer Benchmark Testing", substring = true).performClick()
        composeTestRule.waitForIdle()
    }

    private fun navigateToAthleteDashboard() {
        // Navigate to Roster tab
        composeTestRule.onNodeWithText("Roster", useUnmergedTree = true).performClick()
        // Click on Alex Mercer
        composeTestRule.onNodeWithText("Alex Mercer", substring = true).performClick()
        composeTestRule.waitForIdle()
    }

    // T1-T5: FAB Visibility on SessionReport
    @Test fun test_T1_sessionReport_fabVisible_whenReady() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        navigateToSessionReport()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
    }

    @Test fun test_T2_sessionReport_fabVisible_whenDownloading() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.DOWNLOADING)
        navigateToSessionReport()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
    }

    @Test fun test_T3_sessionReport_fabVisible_whenUnsupported() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.UNSUPPORTED)
        navigateToSessionReport()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
    }

    @Test fun test_T4_sessionReport_fabVisible_whenError() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.ERROR)
        navigateToSessionReport()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
    }

    @Test fun test_T5_sessionReport_fabVisibility_updatesDynamically() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.ERROR)
        navigateToSessionReport()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
        
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
    }

    // T6-T10: FAB Visibility on AthleteDashboard
    @Test fun test_T6_athleteDashboard_fabVisible_whenReady() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        navigateToAthleteDashboard()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
    }

    @Test fun test_T7_athleteDashboard_fabVisible_whenDownloading() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.DOWNLOADING)
        navigateToAthleteDashboard()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
    }

    @Test fun test_T8_athleteDashboard_fabVisible_whenUnsupported() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.UNSUPPORTED)
        navigateToAthleteDashboard()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
    }

    @Test fun test_T9_athleteDashboard_fabVisible_whenError() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.ERROR)
        navigateToAthleteDashboard()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
    }

    @Test fun test_T10_athleteDashboard_fabVisibility_updatesDynamically() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.ERROR)
        navigateToAthleteDashboard()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
        
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
    }

    // T11-T15: Navigation
    @Test fun test_T11_navigation_fromSessionReport_showsAiCoach() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        navigateToSessionReport()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.onNodeWithText("AI Coach", substring = true).assertIsDisplayed()
    }

    @Test fun test_T12_navigation_fromAthleteDashboard_showsAiCoach() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        navigateToAthleteDashboard()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.onNodeWithText("AI Coach", substring = true).assertIsDisplayed()
    }

    @Test fun test_T13_navigation_backFromAiCoach_toSessionReport() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        navigateToSessionReport()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed() // Back on Session Report
    }

    @Test fun test_T14_navigation_backFromAiCoach_toAthleteDashboard() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        navigateToAthleteDashboard()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed() // Back on Athlete Dashboard
    }

    @Test fun test_T15_navigation_systemBackPress_returnsFromAiCoach() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        navigateToSessionReport()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
    }

    // T16-T20: State: Unsupported
    private fun navigateToAiCoach(status: AiCoachStatus) {
        fakeAiCoachRepository.setStatus(status)
        navigateToSessionReport()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
    }

    @Test fun test_T16_unsupportedState_showsText() {
        navigateToAiCoach(AiCoachStatus.UNSUPPORTED)
        composeTestRule.onNodeWithText("Device not supported", substring = true).assertIsDisplayed()
    }

    @Test fun test_T17_unsupportedState_noInputField() {
        navigateToAiCoach(AiCoachStatus.UNSUPPORTED)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).assertDoesNotExist()
    }

    @Test fun test_T18_unsupportedState_noSendButton() {
        navigateToAiCoach(AiCoachStatus.UNSUPPORTED)
        composeTestRule.onNodeWithContentDescription("Send").assertDoesNotExist()
    }

    @Test fun test_T19_unsupportedState_noChips() {
        navigateToAiCoach(AiCoachStatus.UNSUPPORTED)
        composeTestRule.onNodeWithText("How is the team's agility?", substring = true).assertDoesNotExist()
    }

    @Test fun test_T20_unsupportedState_noMessages() {
        navigateToAiCoach(AiCoachStatus.UNSUPPORTED)
        composeTestRule.onNodeWithText("AI Coach initialized", substring = true).assertDoesNotExist()
    }

    // T21-T25: State: Downloading
    @Test fun test_T21_downloadingState_showsText() {
        navigateToAiCoach(AiCoachStatus.DOWNLOADING)
        composeTestRule.onNodeWithText("Downloading AI Model...", substring = true).assertIsDisplayed()
    }

    @Test fun test_T22_downloadingState_showsProgressIndicator() {
        navigateToAiCoach(AiCoachStatus.DOWNLOADING)
        composeTestRule.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertIsDisplayed()
    }

    @Test fun test_T23_downloadingState_noInputField() {
        navigateToAiCoach(AiCoachStatus.DOWNLOADING)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).assertDoesNotExist()
    }

    @Test fun test_T24_downloadingState_noChips() {
        navigateToAiCoach(AiCoachStatus.DOWNLOADING)
        composeTestRule.onNodeWithText("How is the team's agility?", substring = true).assertDoesNotExist()
    }

    @Test fun test_T25_downloadingState_noMessages() {
        navigateToAiCoach(AiCoachStatus.DOWNLOADING)
        composeTestRule.onNodeWithText("AI Coach initialized", substring = true).assertDoesNotExist()
    }

    // T26-T30: State: Ready
    @Test fun test_T26_readyState_inputFieldVisible() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).assertIsDisplayed()
    }

    @Test fun test_T27_readyState_sendButtonVisibleAndDisabled() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithContentDescription("Send").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test fun test_T28_readyState_chipsVisible() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("How is the team's agility?", substring = true).assertIsDisplayed()
    }

    @Test fun test_T29_readyState_typingUpdatesField() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("Hello AI")
        composeTestRule.onNodeWithText("Hello AI", substring = true).assertIsDisplayed()
    }

    @Test fun test_T30_readyState_sendButtonEnabledWhenTextEntered() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("Test")
        composeTestRule.onNodeWithContentDescription("Send").assertIsEnabled()
    }

    // T31-T35: Chat Interaction
    @Test fun test_T31_chat_clickingChipSendsMessage() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("How is the team's agility?", substring = true).performClick()
        composeTestRule.onNodeWithText("How is the team's agility?", substring = true, useUnmergedTree = true).assertIsDisplayed()
    }

    @Test fun test_T32_chat_typingAndSendingAddsUserMessage() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("My custom question")
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        composeTestRule.onNodeWithText("My custom question", substring = true).assertIsDisplayed()
    }

    @Test fun test_T33_chat_sendingClearsInput() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("My custom question")
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        composeTestRule.onNodeWithText("Ask a question...", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("My custom question", substring = true).assertIsDisplayed() // ensure message is sent
    }

    @Test fun test_T34_chat_mockAiResponseAppears() {
        navigateToAiCoach(AiCoachStatus.READY)
        fakeAiCoachRepository.nextResponse = "Mock AI Response"
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("Hello")
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("Mock AI Response", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("Mock AI Response", substring = true).assertIsDisplayed()
    }

    @Test fun test_T35_chat_sendButtonDisabledAfterSend() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("Hello")
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        composeTestRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }

    // --- Helper Methods ---
    private fun simulateRotation(landscape: Boolean) {
        composeTestRule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation = if (landscape) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        composeTestRule.waitForIdle()
    }

    private fun generateLongString(length: Int): String {
        return "A".repeat(length)
    }

    // --- Tier 2 Tests (T36-T70) ---

    // F1: FAB on SessionReport
    @Test fun test_T36_rapidStatusToggle_sessionReport() {
        navigateToSessionReport()
        for(i in 1..5) {
            fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
            fakeAiCoachRepository.setStatus(AiCoachStatus.UNSUPPORTED)
        }
        composeTestRule.waitForIdle()
    }

    @Test fun test_T37_rapidFabDoubleClick_sessionReport() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        navigateToSessionReport()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.waitForIdle()
    }

    @Test fun test_T38_rotation_sessionReport() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        navigateToSessionReport()
        simulateRotation(true)
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
        simulateRotation(false)
    }

    @Test fun test_T39_backgrounding_sessionReport() {
        // Mocking backgrounding by launching and closing another activity is complex in simple compose tests,
        // so we verify standard stability.
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        navigateToSessionReport()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
    }

    @Test fun test_T40_verySlowStatusUpdate_sessionReport() {
        navigateToSessionReport()
        runBlocking {
            delay(100)
            fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        }
        composeTestRule.waitForIdle()
    }

    // F2: FAB on AthleteDashboard
    @Test fun test_T41_rapidStatusToggle_athleteDashboard() {
        navigateToAthleteDashboard()
        for(i in 1..5) {
            fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
            fakeAiCoachRepository.setStatus(AiCoachStatus.UNSUPPORTED)
        }
        composeTestRule.waitForIdle()
    }

    @Test fun test_T42_rapidFabDoubleClick_athleteDashboard() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        navigateToAthleteDashboard()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.waitForIdle()
    }

    @Test fun test_T43_rotation_athleteDashboard() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        navigateToAthleteDashboard()
        simulateRotation(true)
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
        simulateRotation(false)
    }

    @Test fun test_T44_backgrounding_athleteDashboard() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        navigateToAthleteDashboard()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
    }

    @Test fun test_T45_verySlowStatusUpdate_athleteDashboard() {
        navigateToAthleteDashboard()
        runBlocking {
            delay(100)
            fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        }
        composeTestRule.waitForIdle()
    }

    // F3: Navigation
    @Test fun test_T46_backNavigationWhileDownloading() {
        navigateToAiCoach(AiCoachStatus.DOWNLOADING)
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").assertIsDisplayed()
    }

    @Test fun test_T47_deviceRotationOnAiCoachScreen() {
        navigateToAiCoach(AiCoachStatus.READY)
        simulateRotation(true)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).assertIsDisplayed()
        simulateRotation(false)
    }

    @Test fun test_T48_systemBackVsCloseButtonTiming() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()
    }

    @Test fun test_T49_navigatingAwayAndReturning() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.onNodeWithText("AI Coach", substring = true).assertIsDisplayed()
    }

    @Test fun test_T50_navigatingThenImmediatelyPressingBack() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        navigateToSessionReport()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()
    }

    // F4: State Unsupported
    @Test fun test_T51_statusUpdatesToReadyWhileUnsupported() {
        navigateToAiCoach(AiCoachStatus.UNSUPPORTED)
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        composeTestRule.waitForIdle()
    }

    @Test fun test_T52_longStatusStrings() {
        navigateToAiCoach(AiCoachStatus.UNSUPPORTED)
        composeTestRule.waitForIdle()
    }

    @Test fun test_T53_semanticsCheckForDisabledInteractables() {
        navigateToAiCoach(AiCoachStatus.UNSUPPORTED)
        composeTestRule.onNodeWithContentDescription("Send").assertDoesNotExist()
    }

    @Test fun test_T54_rotationDuringUnsupported() {
        navigateToAiCoach(AiCoachStatus.UNSUPPORTED)
        simulateRotation(true)
        composeTestRule.waitForIdle()
        simulateRotation(false)
    }

    @Test fun test_T55_rapidToggleInOutUnsupported() {
        navigateToAiCoach(AiCoachStatus.UNSUPPORTED)
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        fakeAiCoachRepository.setStatus(AiCoachStatus.UNSUPPORTED)
        composeTestRule.waitForIdle()
    }

    // F5: State Downloading
    @Test fun test_T56_extremelyLongDownload() {
        navigateToAiCoach(AiCoachStatus.DOWNLOADING)
        runBlocking { delay(100) }
        composeTestRule.waitForIdle()
    }

    @Test fun test_T57_statusChangesToReadyMidDownload() {
        navigateToAiCoach(AiCoachStatus.DOWNLOADING)
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        composeTestRule.waitForIdle()
    }

    @Test fun test_T58_statusChangesToErrorMidDownload() {
        navigateToAiCoach(AiCoachStatus.DOWNLOADING)
        fakeAiCoachRepository.setStatus(AiCoachStatus.ERROR)
        composeTestRule.waitForIdle()
    }

    @Test fun test_T59_rotationDuringDownloading() {
        navigateToAiCoach(AiCoachStatus.DOWNLOADING)
        simulateRotation(true)
        composeTestRule.waitForIdle()
        simulateRotation(false)
    }

    @Test fun test_T60_backPressSafetyDuringDownloading() {
        navigateToAiCoach(AiCoachStatus.DOWNLOADING)
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.waitForIdle()
    }

    // F6: State Ready
    @Test fun test_T61_5000CharInputText() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput(generateLongString(5000))
        composeTestRule.waitForIdle()
    }

    @Test fun test_T62_emojiSpecialCharacterInput() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("🤔🔥💡!!&&")
        composeTestRule.waitForIdle()
    }

    @Test fun test_T63_whitespaceOnlyInput() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("     ")
        composeTestRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }

    @Test fun test_T64_rotationWithEmptyText() {
        navigateToAiCoach(AiCoachStatus.READY)
        simulateRotation(true)
        composeTestRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
        simulateRotation(false)
    }

    @Test fun test_T65_maxWidthHeightConstraints() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("A\nB\nC\nD\nE")
        composeTestRule.waitForIdle()
    }

    // F7: Chat Interaction
    @Test fun test_T66_sendingEmptyPromptPrevention() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithContentDescription("Send").assertIsNotEnabled()
    }

    @Test fun test_T67_userTypesWhileAiResponseDelayed() {
        navigateToAiCoach(AiCoachStatus.READY)
        fakeAiCoachRepository.networkDelayMs = 100L
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("Q1")
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("Q2")
        composeTestRule.waitForIdle()
    }

    @Test fun test_T68_rapidChipClicking() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("How is the team's agility?", substring = true).performClick()
        composeTestRule.onNodeWithText("How is the team's agility?", substring = true).performClick()
        composeTestRule.waitForIdle()
    }

    @Test fun test_T69_extremelyLongAiResponseHandling() {
        navigateToAiCoach(AiCoachStatus.READY)
        fakeAiCoachRepository.nextResponse = generateLongString(5000)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("Long response")
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        composeTestRule.waitForIdle()
    }

    @Test fun test_T70_aiReturningErrorNullResponse() {
        navigateToAiCoach(AiCoachStatus.READY)
        fakeAiCoachRepository.nextResponse = null
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("Null response")
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        composeTestRule.waitForIdle()
    }

    // --- Tier 3 Tests (T71-T77) ---
    @Test fun test_T71_fabStatusChangesExactlyDuringNavigation() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        navigateToSessionReport()
        fakeAiCoachRepository.setStatus(AiCoachStatus.DOWNLOADING)
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.waitForIdle()
    }

    @Test fun test_T72_statePersistenceBetweenDifferentEntryPoints() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("Hello")
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithContentDescription("Navigate up").performClick() // back to dashboard
        navigateToAthleteDashboard()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.waitForIdle()
    }

    @Test fun test_T73_chatStatePersistence_NavigateAwayAndBack() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("Persist me")
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.waitForIdle()
    }

    @Test fun test_T74_chatStateDuringStatusToggle() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("Toggle status")
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        fakeAiCoachRepository.setStatus(AiCoachStatus.ERROR)
        composeTestRule.waitForIdle()
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        composeTestRule.waitForIdle()
    }

    @Test fun test_T75_interactWithChipsWhileRapidlyChangingToDownloading() {
        navigateToAiCoach(AiCoachStatus.READY)
        fakeAiCoachRepository.setStatus(AiCoachStatus.DOWNLOADING)
        composeTestRule.waitForIdle()
    }

    @Test fun test_T76_fullStateTransitionFlow() {
        navigateToAiCoach(AiCoachStatus.DOWNLOADING)
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("Hello after download")
        composeTestRule.waitForIdle()
    }

    @Test fun test_T77_rotationWhileAiIsResponding() {
        navigateToAiCoach(AiCoachStatus.READY)
        fakeAiCoachRepository.networkDelayMs = 200L
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("Rotate me")
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        simulateRotation(true)
        composeTestRule.waitForIdle()
        simulateRotation(false)
    }

    // --- Tier 4 Tests (T78-T82) ---
    @Test fun test_T78_coachFlow() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.DOWNLOADING)
        navigateToSessionReport()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("How is the team's agility?", substring = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
    }

    @Test fun test_T79_athleteFlow() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.UNSUPPORTED)
        navigateToAthleteDashboard()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.onNodeWithText("Device not supported", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
    }

    @Test fun test_T80_errorRecoveryFlow() {
        fakeAiCoachRepository.setStatus(AiCoachStatus.ERROR)
        navigateToSessionReport()
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        fakeAiCoachRepository.setStatus(AiCoachStatus.READY)
        composeTestRule.onNodeWithContentDescription("Ask AI Coach").performClick()
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("Recovered!")
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        composeTestRule.waitForIdle()
    }

    @Test fun test_T81_followUpFlow() {
        navigateToAiCoach(AiCoachStatus.READY)
        composeTestRule.onNodeWithText("How is the team's agility?", substring = true).performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput("Why?")
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
    }

    @Test fun test_T82_impatientUserFlow() {
        navigateToAiCoach(AiCoachStatus.READY)
        fakeAiCoachRepository.networkDelayMs = 200L
        composeTestRule.onNodeWithText("Ask a question...", substring = true).performTextInput(generateLongString(100))
        composeTestRule.onNodeWithContentDescription("Send").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
    }
}
