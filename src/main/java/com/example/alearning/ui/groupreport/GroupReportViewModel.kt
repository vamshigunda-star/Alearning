package com.example.alearning.ui.groupreport

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.testing.TestingEvent
import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.domain.usecase.testing.GetGroupLeaderboardUseCase
import com.example.alearning.domain.usecase.testing.GetGroupProgressUseCase
import com.example.alearning.domain.usecase.testing.GetRemediationListUseCase
import com.example.alearning.domain.usecase.testing.GroupLeaderboard
import com.example.alearning.domain.usecase.testing.GroupProgress
import com.example.alearning.domain.usecase.testing.RemediationList
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupReportUiState(
    val event: TestingEvent? = null,
    val tests: List<FitnessTest> = emptyList(),
    val selectedTestId: String? = null,
    val leaderboard: GroupLeaderboard? = null,
    val remediationList: RemediationList? = null,
    val selectedTestProgress: GroupProgress? = null,
    val isLoading: Boolean = true,
    val isLeaderboardLoading: Boolean = false,
    val isProgressLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface GroupReportAction {
    data class OnSelectTest(val testId: String) : GroupReportAction
    data class OnNavigateToAthleteReport(val individualId: String) : GroupReportAction
    data class OnNavigateToLeaderboard(val eventId: String, val groupId: String, val mode: String) : GroupReportAction
    data object OnNavigateBack : GroupReportAction
    data object OnDismissError : GroupReportAction
}

@HiltViewModel
class GroupReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val testingRepository: TestingRepository,
    private val standardsRepository: StandardsRepository,
    private val getGroupLeaderboard: GetGroupLeaderboardUseCase,
    private val getRemediationList: GetRemediationListUseCase,
    private val getGroupProgress: GetGroupProgressUseCase
) : ViewModel() {

    val groupId: String = savedStateHandle["groupId"] ?: ""
    val eventId: String? = savedStateHandle["eventId"]

    private val _uiState = MutableStateFlow(GroupReportUiState())
    val uiState: StateFlow<GroupReportUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun onAction(action: GroupReportAction) {
        when (action) {
            is GroupReportAction.OnSelectTest -> {
                loadLeaderboard(action.testId)
                loadProgress(action.testId)
            }
            is GroupReportAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            // Navigation actions handled by screen
            is GroupReportAction.OnNavigateToAthleteReport,
            is GroupReportAction.OnNavigateToLeaderboard,
            is GroupReportAction.OnNavigateBack -> Unit
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                val event = eventId?.let { testingRepository.getEventById(it) }
                val tests = if (eventId != null) {
                    testingRepository.getTestsForEvent(eventId).first()
                } else {
                    standardsRepository.getAllTests().first()
                }
                
                val remediation = eventId?.let { getRemediationList(it, groupId) }

                _uiState.update {
                    it.copy(
                        event = event,
                        tests = tests,
                        remediationList = remediation,
                        isLoading = false
                    )
                }

                // Auto-select first test for leaderboard and progress
                if (tests.isNotEmpty()) {
                    val firstTestId = tests.first().id
                    loadLeaderboard(firstTestId)
                    loadProgress(firstTestId)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }

    private fun loadLeaderboard(testId: String) {
        _uiState.update { it.copy(selectedTestId = testId, isLeaderboardLoading = true) }
        viewModelScope.launch {
            try {
                // If no eventId, we might need a "Global Leaderboard" use case
                // For now, we'll use a placeholder or just skip if no eventId
                val leaderboard = eventId?.let { getGroupLeaderboard(it, testId, groupId) }
                _uiState.update { it.copy(leaderboard = leaderboard, isLeaderboardLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isLeaderboardLoading = false) }
            }
        }
    }

    private fun loadProgress(testId: String) {
        _uiState.update { it.copy(isProgressLoading = true) }
        viewModelScope.launch {
            try {
                val progress = getGroupProgress(groupId, testId)
                _uiState.update { it.copy(selectedTestProgress = progress, isProgressLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isProgressLoading = false) }
            }
        }
    }
}
