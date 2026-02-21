package com.example.alearning.ui.leaderboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.domain.usecase.testing.GetGroupLeaderboardUseCase
import com.example.alearning.domain.usecase.testing.GroupLeaderboard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LeaderboardUiState(
    val tests: List<FitnessTest> = emptyList(),
    val selectedTestId: String? = null,
    val leaderboard: GroupLeaderboard? = null,
    val mode: String = "event",
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface LeaderboardAction {
    data class OnSelectTest(val testId: String) : LeaderboardAction
    data object OnNavigateBack : LeaderboardAction
    data object OnDismissError : LeaderboardAction
}

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val testingRepository: TestingRepository,
    private val getGroupLeaderboard: GetGroupLeaderboardUseCase
) : ViewModel() {

    private val eventId: String = savedStateHandle["eventId"] ?: ""
    private val groupId: String = savedStateHandle["groupId"] ?: ""
    private val mode: String = savedStateHandle["mode"] ?: "event"

    private val _uiState = MutableStateFlow(LeaderboardUiState(mode = mode))
    val uiState: StateFlow<LeaderboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun onAction(action: LeaderboardAction) {
        when (action) {
            is LeaderboardAction.OnSelectTest -> loadLeaderboard(action.testId)
            is LeaderboardAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            is LeaderboardAction.OnNavigateBack -> Unit
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val tests = testingRepository.getTestsForEvent(eventId).first()
                _uiState.update { it.copy(tests = tests, isLoading = false) }
                if (tests.isNotEmpty()) {
                    loadLeaderboard(tests.first().id)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }

    private fun loadLeaderboard(testId: String) {
        _uiState.update { it.copy(selectedTestId = testId) }
        viewModelScope.launch {
            try {
                val leaderboard = getGroupLeaderboard(eventId, testId, groupId)
                _uiState.update { it.copy(leaderboard = leaderboard) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }
}
