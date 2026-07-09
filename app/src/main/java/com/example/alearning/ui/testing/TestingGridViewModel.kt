package com.example.alearning.ui.testing

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.testing.TestResult
import com.example.alearning.domain.model.testing.TestingEvent
import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.domain.usecase.testing.ClearPendingEntryUseCase
import com.example.alearning.domain.usecase.testing.DiscardPendingEntriesUseCase
import com.example.alearning.domain.usecase.testing.GetTestingGridDataUseCase
import com.example.alearning.domain.usecase.testing.ObservePendingEntriesUseCase
import com.example.alearning.domain.usecase.testing.StagePendingEntryUseCase
import com.example.alearning.domain.usecase.testing.SubmitPendingEntriesUseCase
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

enum class CaptureMethodPreference { STOPWATCH, MANUAL }

data class TestingGridUiState(
    val phase: TestingPhase = TestingPhase.EVENT_DETAIL,
    val event: TestingEvent? = null,
    val gridData: TestingGridData? = null,
    val editingCell: EditingCell? = null,
    val timingChoiceCell: TimingChoiceCell? = null,
    // Tracks user's preference for time-based tests in this session: testId -> STOPWATCH or MANUAL
    val testCapturePreferences: Map<String, CaptureMethodPreference> = emptyMap(),
    // Projection of `pending_test_entries` rows for this event, keyed by (athleteId, testId).
    // Source of truth lives in Room; this map is rebuilt on every flow emission.
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

data class TimingChoiceCell(
    val athlete: Individual,
    val test: FitnessTest
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
    data class OnRequestTimingChoice(val athlete: Individual, val test: FitnessTest) : TestingGridAction
    data class OnSelectTimingMethod(val testId: String, val method: CaptureMethodPreference) : TestingGridAction
    data object OnDismissTimingChoice : TestingGridAction
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
    private val observePendingEntries: ObservePendingEntriesUseCase,
    private val stagePendingEntry: StagePendingEntryUseCase,
    private val clearPendingEntry: ClearPendingEntryUseCase,
    private val discardPendingEntries: DiscardPendingEntriesUseCase,
    private val submitPendingEntries: SubmitPendingEntriesUseCase
) : ViewModel() {

    val eventId: String = savedStateHandle["eventId"] ?: ""
    val groupId: String = savedStateHandle["groupId"] ?: ""

    private val _uiState = MutableStateFlow(TestingGridUiState())
    val uiState: StateFlow<TestingGridUiState> = _uiState.asStateFlow()

    // Tracks whether we have already auto-resumed the user into LIVE_ENTRY for a non-empty
    // pending set on the first hydration. Subsequent emptyings (e.g., after Submit/Discard)
    // must NOT bounce them back to EVENT_DETAIL.
    private var hasHydratedPending = false

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
            viewModelScope.launch {
                observePendingEntries(eventId)
                    .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                    .collect { entries ->
                        val map = entries.associate { (it.individualId to it.testId) to it.rawScore }
                        _uiState.update { state ->
                            // Auto-resume into LIVE_ENTRY on first hydration if pending data exists
                            // (coach was previously mid-session, app was killed, now they're back).
                            val resumedPhase = if (!hasHydratedPending && map.isNotEmpty()) {
                                TestingPhase.LIVE_ENTRY
                            } else {
                                state.phase
                            }
                            state.copy(pendingResults = map, phase = resumedPhase)
                        }
                        hasHydratedPending = true
                    }
            }
        }
    }

    fun onAction(action: TestingGridAction) {
        Log.d("TestingGridViewModel", "onAction: $action")
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
            is TestingGridAction.OnConfirmDiscard -> discardAll()
            is TestingGridAction.OnDismissDiscard -> {
                _uiState.update { it.copy(showDiscardDialog = false) }
            }
            is TestingGridAction.OnRequestTimingChoice -> {
                _uiState.update {
                    it.copy(timingChoiceCell = TimingChoiceCell(action.athlete, action.test))
                }
            }
            is TestingGridAction.OnSelectTimingMethod -> {
                _uiState.update {
                    it.copy(
                        testCapturePreferences = it.testCapturePreferences + (action.testId to action.method)
                    )
                }
            }
            is TestingGridAction.OnDismissTimingChoice -> {
                _uiState.update { it.copy(timingChoiceCell = null) }
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
        _uiState.update { it.copy(editingCell = null) }
        viewModelScope.launch {
            try {
                stagePendingEntry(eventId, cell.athlete.id, cell.test.id, rawScore)
            } catch (e: Exception) {
                Log.e(
                    "TestingGridViewModel",
                    "stagePendingEntry FAILED event=$eventId athlete=${cell.athlete.id} test=${cell.test.id} score=$rawScore",
                    e
                )
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    private fun clearEditingCellPending() {
        val cell = _uiState.value.editingCell ?: return
        _uiState.update { it.copy(editingCell = null) }
        viewModelScope.launch {
            try {
                clearPendingEntry(eventId, cell.athlete.id, cell.test.id)
            } catch (e: Exception) {
                Log.e(
                    "TestingGridViewModel",
                    "clearPendingEntry FAILED event=$eventId athlete=${cell.athlete.id} test=${cell.test.id}",
                    e
                )
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    private fun submitAll() {
        val state = _uiState.value
        if (state.pendingResults.isEmpty() || state.isSubmitting) return

        Log.d("TestingGridViewModel", "submitAll start event=$eventId pending=${state.pendingResults.size}")
        _uiState.update { it.copy(isSubmitting = true) }
        viewModelScope.launch {
            val outcome = submitPendingEntries(eventId)
            outcome
                .onSuccess { written ->
                    Log.d("TestingGridViewModel", "submitAll success event=$eventId written=$written")
                    _uiState.update { it.copy(isSubmitting = false) }
                }
                .onFailure { e ->
                    Log.e("TestingGridViewModel", "submitAll FAILED event=$eventId", e)
                    _uiState.update { it.copy(errorMessage = e.message, isSubmitting = false) }
                }
        }
    }

    private fun discardAll() {
        _uiState.update { it.copy(showDiscardDialog = false) }
        viewModelScope.launch {
            try {
                discardPendingEntries(eventId)
            } catch (e: Exception) {
                Log.e("TestingGridViewModel", "discardPendingEntries FAILED event=$eventId", e)
                _uiState.update { it.copy(errorMessage = e.message) }
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
                Log.e(
                    "TestingGridViewModel",
                    "deleteResultById FAILED resultId=${candidate.resultId}",
                    e
                )
                _uiState.update {
                    it.copy(errorMessage = "Delete failed: ${e.message}", deleteCandidate = null)
                }
            }
        }
    }
}
