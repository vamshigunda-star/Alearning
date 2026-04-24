package com.example.alearning.ui.testing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.data.storage.CustomPresetsStore
import com.example.alearning.domain.model.people.Group
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.standards.TestCategory
import com.example.alearning.domain.model.standards.TestPreset
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.usecase.standards.GetTestLibraryUseCase
import com.example.alearning.domain.usecase.testing.CreateEventUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CreateEventUiState(
    val groups: List<Group> = emptyList(),
    val categories: List<TestCategory> = emptyList(),
    val allTests: List<FitnessTest> = emptyList(),
    val availableTests: List<FitnessTest> = emptyList(),
    val presets: List<TestPreset> = emptyList(),
    val selectedGroupId: String? = null,
    val selectedTabIndex: Int = 0,
    val selectedTestIds: Set<String> = emptySet(),
    val eventName: String = "",
    val isSavePresetDialogOpen: Boolean = false,
    val pendingPresetName: String = "",
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
    data class ApplyPreset(val presetId: String) : CreateEventAction
    data class DeletePreset(val presetId: String) : CreateEventAction
    data class SetPendingPresetName(val name: String) : CreateEventAction
    data object OpenSavePresetDialog : CreateEventAction
    data object DismissSavePresetDialog : CreateEventAction
    data object ConfirmSavePreset : CreateEventAction
    data object CreateEvent : CreateEventAction
    data object ClearError : CreateEventAction
    data object NavigationConsumed : CreateEventAction
    data object NavigateBack : CreateEventAction
}

@HiltViewModel
class CreateEventViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val getTestLibrary: GetTestLibraryUseCase,
    private val createEventUseCase: CreateEventUseCase,
    private val customPresetsStore: CustomPresetsStore
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
            combine(getTestLibrary.getCategories(), getTestLibrary.getAllTests()) { categories, tests ->
                categories to tests
            }.collect { (categories, tests) ->
                _uiState.update { state ->
                    val visibleTests = if (categories.isNotEmpty()) {
                        val activeIndex = state.selectedTabIndex.coerceAtMost(categories.lastIndex)
                        val activeCategory = categories[activeIndex]
                        tests.filter { it.categoryId == activeCategory.id }
                    } else {
                        emptyList()
                    }
                    state.copy(
                        categories = categories,
                        allTests = tests,
                        availableTests = visibleTests,
                        presets = buildPresets(categories, tests)
                    )
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
                val tests = _uiState.value.allTests.filter { it.categoryId == category.id }
                _uiState.update { it.copy(selectedTabIndex = action.index, availableTests = tests) }
            }
            is CreateEventAction.ToggleTest -> {
                val current = _uiState.value.selectedTestIds
                _uiState.update {
                    it.copy(selectedTestIds = if (action.testId in current) current - action.testId else current + action.testId)
                }
            }
            is CreateEventAction.ApplyPreset -> applyPreset(action.presetId)
            is CreateEventAction.DeletePreset -> deletePreset(action.presetId)
            is CreateEventAction.SetPendingPresetName -> _uiState.update { it.copy(pendingPresetName = action.name) }
            CreateEventAction.OpenSavePresetDialog -> openSaveDialog()
            CreateEventAction.DismissSavePresetDialog -> _uiState.update {
                it.copy(isSavePresetDialogOpen = false, pendingPresetName = "")
            }
            CreateEventAction.ConfirmSavePreset -> confirmSavePreset()
            CreateEventAction.CreateEvent -> createEvent()
            CreateEventAction.ClearError -> _uiState.update { it.copy(errorMessage = null) }
            CreateEventAction.NavigationConsumed -> _uiState.update { it.copy(eventCreated = null) }
            CreateEventAction.NavigateBack -> Unit
        }
    }

    private fun buildPresets(categories: List<TestCategory>, tests: List<FitnessTest>): List<TestPreset> {
        if (tests.isEmpty()) return emptyList()
        val knownTestIds = tests.map { it.id }.toSet()
        return customPresetsStore.load().map { preset ->
            preset.copy(testIds = preset.testIds.filter { it in knownTestIds })
        }
    }

    private fun applyPreset(presetId: String) {
        val preset = _uiState.value.presets.find { it.id == presetId } ?: return
        val currentlyApplied = _uiState.value.selectedTestIds == preset.testIds.toSet()
        _uiState.update {
            it.copy(selectedTestIds = if (currentlyApplied) emptySet() else preset.testIds.toSet())
        }
    }

    private fun openSaveDialog() {
        if (_uiState.value.selectedTestIds.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Select at least one test before saving a preset") }
            return
        }
        _uiState.update { it.copy(isSavePresetDialogOpen = true, pendingPresetName = "") }
    }

    private fun confirmSavePreset() {
        val state = _uiState.value
        val name = state.pendingPresetName.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Preset name cannot be empty") }
            return
        }
        val preset = TestPreset(
            id = "custom_${UUID.randomUUID()}",
            name = name,
            description = null,
            testIds = state.selectedTestIds.toList(),
            isBuiltIn = false
        )
        customPresetsStore.add(preset)
        _uiState.update {
            it.copy(
                presets = it.presets + preset,
                isSavePresetDialogOpen = false,
                pendingPresetName = ""
            )
        }
    }

    private fun deletePreset(presetId: String) {
        customPresetsStore.delete(presetId)
        _uiState.update { it.copy(presets = it.presets.filterNot { preset -> preset.id == presetId }) }
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
