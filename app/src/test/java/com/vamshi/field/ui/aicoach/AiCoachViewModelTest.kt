package com.example.alearning.ui.aicoach

import androidx.lifecycle.SavedStateHandle
import com.example.alearning.domain.repository.AiCoachRepository
import com.example.alearning.domain.repository.AiCoachStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiCoachViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class TestAiCoachRepository : AiCoachRepository {
        val statusFlow = MutableStateFlow(AiCoachStatus.DOWNLOADING)
        override val status: StateFlow<AiCoachStatus> = statusFlow
        override suspend fun checkAvailability() { statusFlow.value = AiCoachStatus.READY }
        override suspend fun sendMessage(prompt: String, contextData: String?): String = "Response to $prompt"
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialization sets status correctly`() = runTest {
        val repo = TestAiCoachRepository()
        val viewModel = AiCoachViewModel(repo, SavedStateHandle())

        advanceUntilIdle()

        assertEquals(AiCoachStatus.READY, viewModel.uiState.value.status)
    }

    @Test
    fun `sendMessage appends user and AI messages after the greeting`() = runTest {
        val repo = TestAiCoachRepository()
        val viewModel = AiCoachViewModel(repo, SavedStateHandle())

        viewModel.onAction(AiCoachAction.SendMessage("Hello"))
        advanceUntilIdle()

        // Without nav context the VM seeds exactly one greeting message.
        val messages = viewModel.uiState.value.messages
        assertEquals(3, messages.size)
        assertEquals(false, messages[0].isFromUser)
        assertEquals("Hello", messages[1].text)
        assertEquals(true, messages[1].isFromUser)
        assertEquals("Response to Hello", messages[2].text)
        assertEquals(false, messages[2].isFromUser)
    }
}
