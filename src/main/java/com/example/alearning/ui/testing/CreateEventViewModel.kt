package com.example.alearning.ui.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.Group
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.standards.TestCategory
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.usecase.standards.GetTestLibraryUseCase
import com.example.alearning.domain.usecase.testing.CreateEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateEventUiState(
    val groups: List<Group> = emptyList(),
    val categories: List<TestCategory> = emptyList(),
    val availableTests: List<FitnessTest> = emptyList(),
    val selectedGroupId: String? = null,
    val selectedTabIndex: Int = 0,
    val selectedTestIds: Set<String> = emptySet(),
    val eventName: String = "",
    val isLoading: Boolean = true,
    val isCreating: Boolean = false,
    val errorMessage: String? = null,
    val eventCreated: Pair<String, String>? = null  // eventId to groupId, consumed by screen for nav
)

sealed interface CreateEventAction {
    data class SetEventName(val name: String) : CreateEventAction
    data class SelectGroup(val groupId: String) : CreateEventAction
    data class SelectTab(val index: Int) : CreateEventAction
    data class ToggleTest(val testId: String) : CreateEventAction
    data object CreateEvent : CreateEventAction
    data object ClearError : CreateEventAction
    data object NavigationConsumed : CreateEventAction
    data object NavigateBack : CreateEventAction
}

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val getTestLibrary: GetTestLibraryUseCase,
    private val createEventUseCase: CreateEventUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateEventUiState())
    val uiState: StateFlow<CreateEventUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            peopleRepository.getAllGroups().collect { groups ->
                _uiState.update { it.copy(groups = groups, isLoading = false) }
            }
        }
        viewModelScope.launch {
            getTestLibrary.getCategories().collect { categories ->
                _uiState.update { it.copy(categories = categories) }
                if (categories.isNotEmpty()) {
                    loadTestsForCategory(categories.first().id)
                }
            }
        }
    }

    fun onAction(action: CreateEventAction) {
        when (action) {
            is CreateEventAction.SetEventName -> _uiState.update { it.copy(eventName = action.name) }
            is CreateEventAction.SelectGroup -> _uiState.update { it.copy(selectedGroupId = action.groupId) }
            is CreateEventAction.SelectTab -> {
                val category = _uiState.value.categories.getOrNull(action.index) ?: return
                _uiState.update { it.copy(selectedTabIndex = action.index) }
                loadTestsForCategory(category.id)
            }
            is CreateEventAction.ToggleTest -> {
                val current = _uiState.value.selectedTestIds
                _uiState.update {
                    it.copy(selectedTestIds = if (action.testId in current) current - action.testId else current + action.testId)
                }
            }
            CreateEventAction.CreateEvent -> createEvent()
            CreateEventAction.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            CreateEventAction.NavigationConsumed -> _uiState.update { it.copy(eventCreated = null) }
            CreateEventAction.NavigateBack -> Unit
        }
    }

    private fun loadTestsForCategory(categoryId: String) {
        viewModelScope.launch {
            getTestLibrary.getTestsByCategory(categoryId).collect { tests ->
                _uiState.update { it.copy(availableTests = tests) }
            }
        }
    }

    private fun createEvent() {
        val state = _uiState.value
        val groupId = state.selectedGroupId ?: run {
            _uiState.update { it.copy(errorMessage = "Please select a group") }
            return
        }
        if (state.eventName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter an event name") }
            return
        }
        if (state.selectedTestIds.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select at least one test") }
            return
        }

        _uiState.update { it.copy(isCreating = true) }
        viewModelScope.launch {
            val result = createEventUseCase(
                name = state.eventName,
                date = System.currentTimeMillis(),
                groupId = groupId,
                testIds = state.selectedTestIds.toList()
            )
            result.onSuccess { event ->
                _uiState.update { it.copy(isCreating = false, eventCreated = event.id to groupId) }
            }
            result.onFailure { e ->
                _uiState.update { it.copy(isCreating = false, errorMessage = e.message ?: "Failed to create event") }
            }
        }
    }
}
