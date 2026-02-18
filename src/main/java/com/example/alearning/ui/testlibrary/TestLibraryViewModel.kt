package com.example.alearning.ui.testlibrary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.data.local.entities.standards.TestCategoryEntity
import com.example.alearning.domain.usecase.standards.GetTestLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TestLibraryUiState(
    val categories: List<TestCategoryEntity> = emptyList(),
    val selectedCategory: TestCategoryEntity? = null,
    val testsForCategory: List<FitnessTestEntity> = emptyList()
)

@HiltViewModel
class TestLibraryViewModel @Inject constructor(
    private val getTestLibrary: GetTestLibraryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestLibraryUiState())
    val uiState: StateFlow<TestLibraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getTestLibrary.getCategories().collect { categories ->
                _uiState.value = _uiState.value.copy(categories = categories)
                if (_uiState.value.selectedCategory == null && categories.isNotEmpty()) {
                    selectCategory(categories.first())
                }
            }
        }
    }

    fun selectCategory(category: TestCategoryEntity) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        viewModelScope.launch {
            getTestLibrary.getTestsByCategory(category.id).collect { tests ->
                _uiState.value = _uiState.value.copy(testsForCategory = tests)
            }
        }
    }
}
