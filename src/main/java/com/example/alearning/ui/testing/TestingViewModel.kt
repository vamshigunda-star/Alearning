package com.example.alearning.ui.testing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.Group
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.standards.TestCategory
import com.example.alearning.domain.model.testing.TestResult
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.usecase.standards.GetTestLibraryUseCase
import com.example.alearning.domain.usecase.testing.CreateEventUseCase
import com.example.alearning.domain.usecase.testing.GetTestingGridDataUseCase
import com.example.alearning.domain.usecase.testing.RecordTestResultUseCase
import com.example.alearning.domain.usecase.testing.TestingGridData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- Testing Grid State ---

data class TestingGridUiState(
    val gridData: TestingGridData? = null,
    val editingCell: EditingCell? = null,
    val isLoading: Boolean = true
)

data class EditingCell(
    val student: Individual,
    val test: FitnessTest,
    val currentResult: TestResult?
)

@HiltViewModel
class TestingGridViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getGridData: GetTestingGridDataUseCase,
    private val recordResult: RecordTestResultUseCase
) : ViewModel() {

    private val eventId: String = savedStateHandle["eventId"] ?: ""
    private val groupId: String = savedStateHandle["groupId"] ?: ""

    private val _uiState = MutableStateFlow(TestingGridUiState())
    val uiState: StateFlow<TestingGridUiState> = _uiState.asStateFlow()

    init {
        if (eventId.isNotEmpty() && groupId.isNotEmpty()) {
            viewModelScope.launch {
                getGridData.invoke(eventId, groupId).collect { data ->
                    _uiState.value = _uiState.value.copy(gridData = data, isLoading = false)
                }
            }
        }
    }

    fun startEditing(student: Individual, test: FitnessTest) {
        val currentResult = _uiState.value.gridData?.results?.find {
            it.individualId == student.id && it.testId == test.id
        }
        _uiState.value = _uiState.value.copy(
            editingCell = EditingCell(student, test, currentResult)
        )
    }

    fun dismissEditing() {
        _uiState.value = _uiState.value.copy(editingCell = null)
    }

    fun saveScore(rawScore: Double) {
        val cell = _uiState.value.editingCell ?: return
        val student = cell.student
        val ageMillis = System.currentTimeMillis() - student.dateOfBirth
        val ageYears = (ageMillis / (365.25 * 24 * 60 * 60 * 1000)).toFloat()

        viewModelScope.launch {
            recordResult(
                eventId = eventId,
                individualId = student.id,
                testId = cell.test.id,
                rawScore = rawScore,
                ageAtTime = ageYears,
                sex = student.sex
            )
            _uiState.value = _uiState.value.copy(editingCell = null)
        }
    }
}

// --- Create Event State ---

data class CreateEventUiState(
    val groups: List<Group> = emptyList(),
    val categories: List<TestCategory> = emptyList(),
    val availableTests: List<FitnessTest> = emptyList(),
    val selectedGroupId: String? = null,
    val selectedTestIds: Set<String> = emptySet(),
    val eventName: String = "",
    val isCreating: Boolean = false
)

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val getTestLibrary: GetTestLibraryUseCase,
    private val createEvent: CreateEventUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            peopleRepository.getAllGroups().collect { groups ->
                _uiState.value = _uiState.value.copy(groups = groups)
            }
        }
        viewModelScope.launch {
            getTestLibrary.getCategories().collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
                if (categories.isNotEmpty()) {
                    loadTestsForCategory(categories.first().id)
                }
            }
        }
    }

    fun loadTestsForCategory(categoryId: String) {
        viewModelScope.launch {
            getTestLibrary.getTestsByCategory(categoryId).collect { tests ->
                _uiState.value = _uiState.value.copy(availableTests = tests)
            }
        }
    }

    fun setEventName(name: String) {
        _uiState.value = _uiState.value.copy(eventName = name)
    }

    fun selectGroup(groupId: String) {
        _uiState.value = _uiState.value.copy(selectedGroupId = groupId)
    }

    fun toggleTest(testId: String) {
        val current = _uiState.value.selectedTestIds
        _uiState.value = _uiState.value.copy(
            selectedTestIds = if (testId in current) current - testId else current + testId
        )
    }

    fun createEvent(onSuccess: (eventId: String, groupId: String) -> Unit) {
        val state = _uiState.value
        val groupId = state.selectedGroupId ?: return

        _uiState.value = state.copy(isCreating = true)
        viewModelScope.launch {
            val result = createEvent(
                name = state.eventName,
                date = System.currentTimeMillis(),
                groupId = groupId,
                testIds = state.selectedTestIds.toList()
            )
            result.onSuccess { event ->
                onSuccess(event.id, groupId)
            }
            _uiState.value = _uiState.value.copy(isCreating = false)
        }
    }
}
