package com.example.alearning.ui.quicktest

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.BiologicalSex
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
import com.example.alearning.domain.usecase.standards.CalculatePercentileUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

enum class QuickTestStep { SETUP, ENTER_SCORES, COMPLETE }

data class RecordedTestResult(
    val id: String? = null,
    val testId: String,
    val testName: String,
    val unit: String,
    val isHigherBetter: Boolean,
    val rawScore: Double,
    val percentile: Int?,
    val classification: String?
)

data class QuickTestUiState(
    val step: QuickTestStep = QuickTestStep.SETUP,
    val eventName: String = "",
    val allAthletes: List<Individual> = emptyList(),
    val athleteSearchQuery: String = "",
    val matchingAthletes: List<Individual> = emptyList(),
    val selectedAthlete: Individual? = null,
    val isGuest: Boolean = false,
    val guestSex: BiologicalSex = BiologicalSex.UNSPECIFIED,
    val guestAge: String = "",
    val categories: List<TestCategory> = emptyList(),
    val selectedCategoryIndex: Int = 0,
    val availableTests: List<FitnessTest> = emptyList(),
    val selectedTestIds: Set<String> = emptySet(),
    val selectedTests: List<FitnessTest> = emptyList(),
    val recordedResults: List<RecordedTestResult> = emptyList(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val errorMessage: String? = null
)

sealed interface QuickTestAction {
    data class OnSetEventName(val name: String) : QuickTestAction
    data class OnAthleteQueryChange(val query: String) : QuickTestAction
    data class OnSelectAthlete(val athlete: Individual?) : QuickTestAction
    data class OnSetGuestSex(val sex: BiologicalSex) : QuickTestAction
    data class OnSetGuestAge(val age: String) : QuickTestAction
    data class OnSelectCategory(val index: Int) : QuickTestAction
    data class OnToggleTest(val testId: String) : QuickTestAction
    data object OnConfirmSetup : QuickTestAction
    data class OnSaveScore(val testId: String, val rawScore: Double) : QuickTestAction
    data class OnDeleteScore(val testId: String) : QuickTestAction
    data object OnCompleteTest : QuickTestAction
    data object OnRequestDelete : QuickTestAction
    data object OnConfirmDelete : QuickTestAction
    data object OnDismissDelete : QuickTestAction
    data object OnNavigateBack : QuickTestAction
    data object OnDismissError : QuickTestAction
}

@HiltViewModel
class QuickTestViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val peopleRepository: PeopleRepository,
    private val getTestLibrary: GetTestLibraryUseCase,
    private val createEventUseCase: CreateEventUseCase,
    private val recordResult: RecordTestResultUseCase,
    private val calculatePercentile: CalculatePercentileUseCase,
    private val testingRepository: com.example.alearning.domain.repository.TestingRepository
) : ViewModel() {

    private val athleteId: String? = savedStateHandle["athleteId"]
    private val initialTestIds: Set<String> = savedStateHandle.get<String>("testIds")
        ?.split(",")
        ?.mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } }
        ?.toSet()
        ?: emptySet()

    private val _uiState = MutableStateFlow(
        QuickTestUiState(selectedTestIds = initialTestIds)
    )
    val uiState: StateFlow<QuickTestUiState> = _uiState.asStateFlow()

    private var hasInitializedAthlete: Boolean = false

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Load Categories
                getTestLibrary.getCategories().collect { categories ->
                    _uiState.update { it.copy(categories = categories) }
                    if (categories.isNotEmpty()) {
                        loadTests(categories.first().id)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }

        viewModelScope.launch {
            peopleRepository.getAllIndividuals().collect { athletes ->
                if (!hasInitializedAthlete) {
                    val selectedAthlete = athleteId?.let { id -> athletes.find { it.id == id } }
                    _uiState.update {
                        it.copy(
                            allAthletes = athletes,
                            isLoading = false,
                            selectedAthlete = selectedAthlete,
                            athleteSearchQuery = selectedAthlete?.fullName ?: ""
                        )
                    }
                    hasInitializedAthlete = true
                } else {
                    _uiState.update { it.copy(allAthletes = athletes) }
                }
            }
        }
    }

    fun onAction(action: QuickTestAction) {
        when (action) {
            is QuickTestAction.OnSetEventName -> _uiState.update { it.copy(eventName = action.name) }
            is QuickTestAction.OnAthleteQueryChange -> {
                val query = action.query
                val matching = if (query.isBlank()) emptyList() else {
                    _uiState.value.allAthletes.filter { 
                        it.firstName.contains(query, ignoreCase = true) || 
                        it.lastName.contains(query, ignoreCase = true) 
                    }
                }
                _uiState.update { it.copy(athleteSearchQuery = query, matchingAthletes = matching, selectedAthlete = null) }
            }
            is QuickTestAction.OnSelectAthlete -> {
                _uiState.update { it.copy(
                    selectedAthlete = action.athlete, 
                    athleteSearchQuery = action.athlete?.fullName ?: "",
                    matchingAthletes = emptyList(),
                    isGuest = action.athlete == null
                ) }
            }
            is QuickTestAction.OnSetGuestSex -> _uiState.update { it.copy(guestSex = action.sex) }
            is QuickTestAction.OnSetGuestAge -> _uiState.update { it.copy(guestAge = action.age) }
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
            is QuickTestAction.OnConfirmSetup -> {
                val state = _uiState.value
                if (state.athleteSearchQuery.isBlank()) {
                    _uiState.update { it.copy(errorMessage = "Please enter a name") }
                    return
                }
                if (state.selectedTestIds.isEmpty()) {
                    _uiState.update { it.copy(errorMessage = "Select at least one test") }
                    return
                }
                confirmSetupAndProceed()
            }
            is QuickTestAction.OnSaveScore -> saveScore(action.testId, action.rawScore)
            is QuickTestAction.OnDeleteScore -> deleteScore(action.testId)
            is QuickTestAction.OnCompleteTest -> _uiState.update { it.copy(step = QuickTestStep.COMPLETE) }
            is QuickTestAction.OnRequestDelete -> _uiState.update { it.copy(showDeleteConfirmation = true) }
            is QuickTestAction.OnConfirmDelete -> confirmDelete()
            is QuickTestAction.OnDismissDelete -> _uiState.update { it.copy(showDeleteConfirmation = false) }
            is QuickTestAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            is QuickTestAction.OnNavigateBack -> {
                val currentState = _uiState.value
                if (currentState.step == QuickTestStep.ENTER_SCORES) {
                    _uiState.update { it.copy(step = QuickTestStep.SETUP) }
                } else if (currentState.step == QuickTestStep.COMPLETE) {
                    // Navigate handled in UI
                }
            }
        }
    }

    private fun confirmDelete() {
        val eventId = createdEventId ?: return
        _uiState.update { it.copy(isDeleting = true, showDeleteConfirmation = false) }
        viewModelScope.launch {
            try {
                val results = testingRepository.getEventResults(eventId).first()
                for (res in results) {
                    testingRepository.deleteResultById(res.id)
                }
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        step = QuickTestStep.SETUP,
                        selectedTestIds = emptySet(),
                        recordedResults = emptyList()
                    )
                }
                createdEventId = null
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Delete failed: ${e.message}", isDeleting = false) }
            }
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

    private fun confirmSetupAndProceed() {
        val state = _uiState.value
        viewModelScope.launch {
            try {
                val allTests = mutableListOf<FitnessTest>()
                for (category in state.categories) {
                    val tests = getTestLibrary.getTestsByCategory(category.id).first()
                    allTests.addAll(tests.filter { it.id in state.selectedTestIds })
                }
                _uiState.update {
                    it.copy(
                        selectedTests = allTests, 
                        step = QuickTestStep.ENTER_SCORES,
                        isGuest = state.selectedAthlete == null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }

    private fun saveScore(testId: String, rawScore: Double) {
        val state = _uiState.value
        val test = state.selectedTests.find { it.id == testId } ?: return

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val recorded: RecordedTestResult = if (state.selectedAthlete != null) {
                    val athlete = state.selectedAthlete
                    val eventId = ensureEventExists()
                    val ageMillis = System.currentTimeMillis() - athlete.dateOfBirth
                    val ageYears = (ageMillis / (365.25 * 24 * 60 * 60 * 1000)).toFloat()

                    val result = recordResult(
                        eventId = eventId,
                        individualId = athlete.id,
                        testId = test.id,
                        rawScore = rawScore,
                        ageAtTime = ageYears,
                        sex = athlete.sex
                    )
                    RecordedTestResult(
                        id = result.id,
                        testId = test.id,
                        testName = test.name,
                        unit = test.unit,
                        isHigherBetter = test.isHigherBetter,
                        rawScore = rawScore,
                        percentile = result.percentile,
                        classification = result.classification
                    )
                } else {
                    val age = state.guestAge.toDoubleOrNull() ?: 18.0
                    val pr = calculatePercentile(test.id, rawScore, age, state.guestSex)
                    RecordedTestResult(
                        testId = test.id,
                        testName = test.name,
                        unit = test.unit,
                        isHigherBetter = test.isHigherBetter,
                        rawScore = rawScore,
                        percentile = pr?.percentile,
                        classification = pr?.classification
                    )
                }

                _uiState.update {
                    val newResults = it.recordedResults.filter { r -> r.testId != testId } + recorded
                    it.copy(recordedResults = newResults, isSaving = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isSaving = false) }
            }
        }
    }

    private fun deleteScore(testId: String) {
        val state = _uiState.value
        val existingResult = state.recordedResults.find { it.testId == testId } ?: return
        
        viewModelScope.launch {
            try {
                if (existingResult.id != null) {
                    testingRepository.deleteResultById(existingResult.id)
                }
                _uiState.update { 
                    it.copy(recordedResults = it.recordedResults.filter { r -> r.testId != testId }) 
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Delete failed: ${e.message}") }
            }
        }
    }

    private var createdEventId: String? = savedStateHandle.get<String>("eventId")

    private suspend fun ensureEventExists(): String {
        createdEventId?.let { return it }

        val state = _uiState.value
        val eventName = state.eventName.ifBlank {
            val dateFormat = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault())
            "Quick Test - ${dateFormat.format(Date())}"
        }

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


