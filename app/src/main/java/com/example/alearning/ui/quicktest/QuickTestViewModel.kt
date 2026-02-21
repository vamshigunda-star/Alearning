package com.example.alearning.ui.quicktest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.standards.TestCategory
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.usecase.standards.GetTestLibraryUseCase
import com.example.alearning.domain.usecase.testing.CreateEventUseCase
import com.example.alearning.domain.usecase.testing.RecordTestResultUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

enum class QuickTestStep { SELECT_ATHLETE, SELECT_TESTS, ENTER_SCORES, COMPLETE }

data class QuickTestUiState(
    val step: QuickTestStep = QuickTestStep.SELECT_ATHLETE,
    val allAthletes: List<Individual> = emptyList(),
    val searchQuery: String = "",
    val selectedAthlete: Individual? = null,
    val categories: List<TestCategory> = emptyList(),
    val selectedCategoryIndex: Int = 0,
    val availableTests: List<FitnessTest> = emptyList(),
    val selectedTestIds: Set<String> = emptySet(),
    val selectedTests: List<FitnessTest> = emptyList(),
    val currentTestIndex: Int = 0,
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface QuickTestAction {
    data class OnSearchQueryChange(val query: String) : QuickTestAction
    data class OnSelectAthlete(val athlete: Individual) : QuickTestAction
    data class OnSelectCategory(val index: Int) : QuickTestAction
    data class OnToggleTest(val testId: String) : QuickTestAction
    data object OnConfirmTests : QuickTestAction
    data class OnSaveScore(val rawScore: Double) : QuickTestAction
    data object OnSkipTest : QuickTestAction
    data object OnNavigateBack : QuickTestAction
    data object OnDismissError : QuickTestAction
}

@HiltViewModel
class QuickTestViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val getTestLibrary: GetTestLibraryUseCase,
    private val createEventUseCase: CreateEventUseCase,
    private val recordResult: RecordTestResultUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuickTestUiState())
    val uiState: StateFlow<QuickTestUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            peopleRepository.getAllIndividuals()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
                .collect { athletes ->
                    _uiState.update { it.copy(allAthletes = athletes, isLoading = false) }
                }
        }
        viewModelScope.launch {
            getTestLibrary.getCategories()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { categories ->
                    _uiState.update { it.copy(categories = categories) }
                    if (categories.isNotEmpty()) {
                        loadTests(categories.first().id)
                    }
                }
        }
    }

    fun onAction(action: QuickTestAction) {
        when (action) {
            is QuickTestAction.OnSearchQueryChange -> _uiState.update { it.copy(searchQuery = action.query) }
            is QuickTestAction.OnSelectAthlete -> {
                _uiState.update { it.copy(selectedAthlete = action.athlete, step = QuickTestStep.SELECT_TESTS) }
            }
            is QuickTestAction.OnSelectCategory -> {
                val category = _uiState.value.categories.getOrNull(action.index) ?: return
                _uiState.update { it.copy(selectedCategoryIndex = action.index) }
                loadTests(category.id)
            }
            is QuickTestAction.OnToggleTest -> {
                val current = _uiState.value.selectedTestIds
                _uiState.update {
                    it.copy(selectedTestIds = if (action.testId in current) current - action.testId else current + action.testId)
                }
            }
            is QuickTestAction.OnConfirmTests -> confirmTestsAndStartScoring()
            is QuickTestAction.OnSaveScore -> saveScoreAndAdvance(action.rawScore)
            is QuickTestAction.OnSkipTest -> skipTest()
            is QuickTestAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            is QuickTestAction.OnNavigateBack -> Unit
        }
    }

    private fun loadTests(categoryId: String) {
        viewModelScope.launch {
            getTestLibrary.getTestsByCategory(categoryId)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { tests ->
                    _uiState.update { it.copy(availableTests = tests) }
                }
        }
    }

    private fun confirmTestsAndStartScoring() {
        val state = _uiState.value
        if (state.selectedTestIds.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Select at least one test") }
            return
        }

        // Collect the full FitnessTest objects for selected tests
        // We need to load all tests across categories for selected IDs
        viewModelScope.launch {
            try {
                val allTests = mutableListOf<FitnessTest>()
                for (category in state.categories) {
                    val tests = getTestLibrary.getTestsByCategory(category.id).first()
                    allTests.addAll(tests.filter { it.id in state.selectedTestIds })
                }
                _uiState.update {
                    it.copy(selectedTests = allTests, step = QuickTestStep.ENTER_SCORES, currentTestIndex = 0)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    private fun saveScoreAndAdvance(rawScore: Double) {
        val state = _uiState.value
        val athlete = state.selectedAthlete ?: return
        val test = state.selectedTests.getOrNull(state.currentTestIndex) ?: return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                // Auto-create event on first score
                val eventId = ensureEventExists()

                val ageMillis = System.currentTimeMillis() - athlete.dateOfBirth
                val ageYears = (ageMillis / (365.25 * 24 * 60 * 60 * 1000)).toFloat()

                recordResult(
                    eventId = eventId,
                    individualId = athlete.id,
                    testId = test.id,
                    rawScore = rawScore,
                    ageAtTime = ageYears,
                    sex = athlete.sex
                )

                advanceToNextTest()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isSaving = false) }
            }
        }
    }

    private fun skipTest() {
        advanceToNextTest()
    }

    private fun advanceToNextTest() {
        val state = _uiState.value
        val nextIndex = state.currentTestIndex + 1
        if (nextIndex >= state.selectedTests.size) {
            _uiState.update { it.copy(step = QuickTestStep.COMPLETE, isSaving = false) }
        } else {
            _uiState.update { it.copy(currentTestIndex = nextIndex, isSaving = false) }
        }
    }

    private var createdEventId: String? = null

    private suspend fun ensureEventExists(): String {
        createdEventId?.let { return it }

        val state = _uiState.value
        val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
        val eventName = "Quick Test - ${dateFormat.format(Date())}"

        val result = createEventUseCase(
            name = eventName,
            date = System.currentTimeMillis(),
            groupId = null,
            testIds = state.selectedTestIds.toList()
        )

        val event = result.getOrThrow()
        createdEventId = event.id
        return event.id
    }
}
