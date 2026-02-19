package com.example.alearning.ui.testlibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.standards.TestCategory
import com.example.alearning.domain.usecase.standards.GetTestLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TestLibraryUiState(
    val categories: List<TestCategory> = emptyList(),
    val selectedCategory: TestCategory? = null,
    val testsForCategory: List<FitnessTest> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface TestLibraryAction {
    data class OnSelectCategory(val category: TestCategory) : TestLibraryAction
    data object OnNavigateBack : TestLibraryAction
    data object OnDismissError : TestLibraryAction
}

@HiltViewModel
class TestLibraryViewModel @Inject constructor(
    private val getTestLibrary: GetTestLibraryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestLibraryUiState())
    val uiState: StateFlow<TestLibraryUiState> = _uiState.asStateFlow()

    private var testsJob: Job? = null

    init {
        viewModelScope.launch {
            getTestLibrary.getCategories()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
                .collect { categories ->
                    _uiState.update { current ->
                        val selected = current.selectedCategory ?: categories.firstOrNull()
                        current.copy(categories = categories, selectedCategory = selected, isLoading = false)
                    }
                    val selected = _uiState.value.selectedCategory
                    if (selected != null && testsJob == null) {
                        loadTests(selected.id)
                    }
                }
        }
    }

    fun onAction(action: TestLibraryAction) {
        when (action) {
            is TestLibraryAction.OnSelectCategory -> {
                _uiState.update { it.copy(selectedCategory = action.category) }
                loadTests(action.category.id)
            }
            is TestLibraryAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            is TestLibraryAction.OnNavigateBack -> Unit
        }
    }

    private fun loadTests(categoryId: String) {
        testsJob?.cancel()
        testsJob = viewModelScope.launch {
            getTestLibrary.getTestsByCategory(categoryId)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { tests ->
                    _uiState.update { it.copy(testsForCategory = tests) }
                }
        }
    }
}
