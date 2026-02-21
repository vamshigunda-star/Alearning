package com.example.alearning.ui.testing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.testing.TestResult
import com.example.alearning.domain.model.testing.TestingEvent
import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.domain.usecase.testing.GetTestingGridDataUseCase
import com.example.alearning.domain.usecase.testing.RecordTestResultUseCase
import com.example.alearning.domain.usecase.testing.TestingGridData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TestingPhase { EVENT_DETAIL, LIVE_ENTRY }

data class TestingGridUiState(
    val phase: TestingPhase = TestingPhase.EVENT_DETAIL,
    val event: TestingEvent? = null,
    val gridData: TestingGridData? = null,
    val editingCell: EditingCell? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

data class EditingCell(
    val athlete: Individual,
    val test: FitnessTest,
    val currentResult: TestResult?
)

sealed interface TestingGridAction {
    data object OnStartTesting : TestingGridAction
    data class OnStartEditing(val athlete: Individual, val test: FitnessTest) : TestingGridAction
    data object OnDismissEditing : TestingGridAction
    data class OnSaveScore(val rawScore: Double) : TestingGridAction
    data object OnDismissError : TestingGridAction
    // Navigation actions — handled by the screen composable
    data object OnNavigateBack : TestingGridAction
    data class OnNavigateToAthleteReport(val individualId: String) : TestingGridAction
    data class OnNavigateToLeaderboard(val eventId: String, val groupId: String, val mode: String) : TestingGridAction
    data class OnNavigateToGroupReport(val eventId: String, val groupId: String) : TestingGridAction
}

@HiltViewModel
class TestingGridViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val testingRepository: TestingRepository,
    private val getGridData: GetTestingGridDataUseCase,
    private val recordResult: RecordTestResultUseCase
) : ViewModel() {

    val eventId: String = savedStateHandle["eventId"] ?: ""
    val groupId: String = savedStateHandle["groupId"] ?: ""

    private val _uiState = MutableStateFlow(TestingGridUiState())
    val uiState: StateFlow<TestingGridUiState> = _uiState.asStateFlow()

    init {
        if (eventId.isNotEmpty() && groupId.isNotEmpty()) {
            viewModelScope.launch {
                val event = testingRepository.getEventById(eventId)
                _uiState.update { it.copy(event = event) }
            }
            viewModelScope.launch {
                getGridData.invoke(eventId, groupId)
                    .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
                    .collect { data ->
                        _uiState.update { it.copy(gridData = data, isLoading = false) }
                    }
            }
        }
    }

    fun onAction(action: TestingGridAction) {
        when (action) {
            is TestingGridAction.OnStartTesting -> {
                _uiState.update { it.copy(phase = TestingPhase.LIVE_ENTRY) }
            }
            is TestingGridAction.OnStartEditing -> {
                val currentResult = _uiState.value.gridData?.results?.find {
                    it.individualId == action.athlete.id && it.testId == action.test.id
                }
                _uiState.update {
                    it.copy(editingCell = EditingCell(action.athlete, action.test, currentResult))
                }
            }
            is TestingGridAction.OnDismissEditing -> {
                _uiState.update { it.copy(editingCell = null) }
            }
            is TestingGridAction.OnSaveScore -> saveScore(action.rawScore)
            is TestingGridAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            // Navigation actions handled by screen composable
            is TestingGridAction.OnNavigateBack,
            is TestingGridAction.OnNavigateToAthleteReport,
            is TestingGridAction.OnNavigateToLeaderboard,
            is TestingGridAction.OnNavigateToGroupReport -> Unit
        }
    }

    private fun saveScore(rawScore: Double) {
        val cell = _uiState.value.editingCell ?: return
        val athlete = cell.athlete
        val ageMillis = System.currentTimeMillis() - athlete.dateOfBirth
        val ageYears = (ageMillis / (365.25 * 24 * 60 * 60 * 1000)).toFloat()
        viewModelScope.launch {
            try {
                recordResult(
                    eventId = eventId,
                    individualId = athlete.id,
                    testId = cell.test.id,
                    rawScore = rawScore,
                    ageAtTime = ageYears,
                    sex = athlete.sex
                )
                _uiState.update { it.copy(editingCell = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, editingCell = null) }
            }
        }
    }
}
