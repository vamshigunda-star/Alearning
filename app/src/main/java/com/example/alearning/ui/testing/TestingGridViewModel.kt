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
    // Map of (athleteId, testId) -> raw score waiting for explicit submit.
    // Pending entries are NOT persisted until OnSubmitAll fires.
    val pendingResults: Map<Pair<String, String>, Double> = emptyMap(),
    val isSubmitting: Boolean = false,
    val showDiscardDialog: Boolean = false,
    val deleteCandidate: DeleteCandidate? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val hasPendingChanges: Boolean get() = pendingResults.isNotEmpty()
}

data class EditingCell(
    val athlete: Individual,
    val test: FitnessTest,
    val currentResult: TestResult?,
    val pendingScore: Double?
)

data class DeleteCandidate(
    val athlete: Individual,
    val test: FitnessTest,
    val resultId: String
)

sealed interface TestingGridAction {
    data object OnStartTesting : TestingGridAction
    data class OnStartEditing(val athlete: Individual, val test: FitnessTest) : TestingGridAction
    data object OnDismissEditing : TestingGridAction
    data class OnSaveScore(val rawScore: Double) : TestingGridAction
    data object OnClearPendingForEditingCell : TestingGridAction
    data object OnSubmitAll : TestingGridAction
    data object OnRequestBack : TestingGridAction
    data object OnConfirmDiscard : TestingGridAction
    data object OnDismissDiscard : TestingGridAction
    data class OnRequestDelete(val athlete: Individual, val test: FitnessTest, val resultId: String) : TestingGridAction
    data object OnConfirmDelete : TestingGridAction
    data object OnDismissDelete : TestingGridAction
    data object OnDismissError : TestingGridAction
    // Navigation actions — handled by the screen composable
    data object OnNavigateBack : TestingGridAction
    data class OnNavigateToAthleteReport(val individualId: String) : TestingGridAction
    data class OnNavigateToLeaderboard(val eventId: String, val groupId: String, val mode: String) : TestingGridAction
    data class OnNavigateToGroupReport(val eventId: String, val groupId: String) : TestingGridAction
    data class OnNavigateToStopwatch(val eventId: String, val fitnessTestId: String, val groupId: String, val individualId: String?) : TestingGridAction
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
                val state = _uiState.value
                val currentResult = state.gridData?.results?.find {
                    it.individualId == action.athlete.id && it.testId == action.test.id
                }
                val pending = state.pendingResults[action.athlete.id to action.test.id]
                _uiState.update {
                    it.copy(
                        editingCell = EditingCell(
                            athlete = action.athlete,
                            test = action.test,
                            currentResult = currentResult,
                            pendingScore = pending
                        )
                    )
                }
            }
            is TestingGridAction.OnDismissEditing -> {
                _uiState.update { it.copy(editingCell = null) }
            }
            is TestingGridAction.OnSaveScore -> stagePendingScore(action.rawScore)
            is TestingGridAction.OnClearPendingForEditingCell -> clearEditingCellPending()
            is TestingGridAction.OnSubmitAll -> submitAll()
            is TestingGridAction.OnRequestBack -> {
                if (_uiState.value.hasPendingChanges) {
                    _uiState.update { it.copy(showDiscardDialog = true) }
                }
            }
            is TestingGridAction.OnConfirmDiscard -> {
                _uiState.update { it.copy(pendingResults = emptyMap(), showDiscardDialog = false) }
            }
            is TestingGridAction.OnDismissDiscard -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
            }
            is TestingGridAction.OnRequestDelete -> {
                _uiState.update {
                    it.copy(
                        deleteCandidate = DeleteCandidate(action.athlete, action.test, action.resultId),
                        editingCell = null
                    )
                }
            }
            is TestingGridAction.OnConfirmDelete -> deleteResult()
            is TestingGridAction.OnDismissDelete -> _uiState.update { it.copy(deleteCandidate = null) }
            is TestingGridAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            // Navigation actions handled by screen composable
            is TestingGridAction.OnNavigateBack,
            is TestingGridAction.OnNavigateToAthleteReport,
            is TestingGridAction.OnNavigateToLeaderboard,
            is TestingGridAction.OnNavigateToGroupReport,
            is TestingGridAction.OnNavigateToStopwatch -> Unit
        }
    }

    private fun stagePendingScore(rawScore: Double) {
        val cell = _uiState.value.editingCell ?: return
        val key = cell.athlete.id to cell.test.id
        _uiState.update {
            it.copy(
                pendingResults = it.pendingResults + (key to rawScore),
                editingCell = null
            )
        }
    }

    private fun clearEditingCellPending() {
        val cell = _uiState.value.editingCell ?: return
        val key = cell.athlete.id to cell.test.id
        _uiState.update {
            it.copy(
                pendingResults = it.pendingResults - key,
                editingCell = null
            )
        }
    }

    private fun submitAll() {
        val state = _uiState.value
        val pending = state.pendingResults
        val gridData = state.gridData ?: return
        if (pending.isEmpty() || state.isSubmitting) return

        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            try {
                for ((key, rawScore) in pending) {
                    val (athleteId, testId) = key
                    val athlete = gridData.students.firstOrNull { it.id == athleteId } ?: continue
                    val test = gridData.tests.firstOrNull { it.id == testId } ?: continue
                    val ageMillis = System.currentTimeMillis() - athlete.dateOfBirth
                    val ageYears = (ageMillis / (365.25 * 24 * 60 * 60 * 1000)).toFloat()
                    recordResult(
                        eventId = eventId,
                        individualId = athleteId,
                        testId = testId,
                        rawScore = rawScore,
                        ageAtTime = ageYears,
                        sex = athlete.sex
                    )
                }
                _uiState.update {
                    it.copy(pendingResults = emptyMap(), isSubmitting = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isSubmitting = false) }
            }
        }
    }

    private fun deleteResult() {
        val candidate = _uiState.value.deleteCandidate ?: return
        viewModelScope.launch {
            try {
                testingRepository.deleteResultById(candidate.resultId)
                _uiState.update { it.copy(deleteCandidate = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Delete failed: ${e.message}", deleteCandidate = null)
                }
            }
        }
    }
}
