package com.example.alearning.ui.athletes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.usecase.people.RegisterAthleteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AthletesUiState(
    val allAthletes: List<Individual> = emptyList(),
    val filteredAthletes: List<Individual> = emptyList(),
    val searchQuery: String = "",
    val sexFilter: BiologicalSex? = null,
    val showRegisterSheet: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface AthletesAction {
    data class OnSearchQueryChange(val query: String) : AthletesAction
    data class OnSexFilterChange(val sex: BiologicalSex?) : AthletesAction
    data object OnShowRegisterSheet : AthletesAction
    data object OnDismissRegisterSheet : AthletesAction
    data class OnRegisterAthlete(
        val firstName: String,
        val lastName: String,
        val dateOfBirth: Long,
        val sex: BiologicalSex,
        val email: String?,
        val medicalAlert: String?
    ) : AthletesAction
    data class OnNavigateToAthleteReport(val individualId: String) : AthletesAction
    data object OnDismissError : AthletesAction
}

@HiltViewModel
class AthletesViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val registerAthleteUseCase: RegisterAthleteUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AthletesUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            peopleRepository.getAllIndividuals().collect { athletes ->
                _uiState.update { it.copy(allAthletes = athletes, isLoading = false) }
                applyFilters()
            }
        }
    }

    fun onAction(action: AthletesAction) {
        when (action) {
            is AthletesAction.OnSearchQueryChange -> {
                _uiState.update { it.copy(searchQuery = action.query) }
                applyFilters()
            }
            is AthletesAction.OnSexFilterChange -> {
                _uiState.update { it.copy(sexFilter = action.sex) }
                applyFilters()
            }
            AthletesAction.OnShowRegisterSheet -> _uiState.update { it.copy(showRegisterSheet = true) }
            AthletesAction.OnDismissRegisterSheet -> _uiState.update { it.copy(showRegisterSheet = false) }
            is AthletesAction.OnRegisterAthlete -> {
                viewModelScope.launch {
                    try {
                        registerAthleteUseCase(
                            firstName = action.firstName,
                            lastName = action.lastName,
                            dateOfBirth = action.dateOfBirth,
                            sex = action.sex,
                            email = action.email,
                            medicalAlert = action.medicalAlert
                        )
                        _uiState.update { it.copy(showRegisterSheet = false) }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(errorMessage = e.message ?: "Failed to register athlete") }
                    }
                }
            }
            AthletesAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            is AthletesAction.OnNavigateToAthleteReport -> {} // Handled by Screen
        }
    }

    private fun applyFilters() {
        val state = _uiState.value
        val query = state.searchQuery.trim()
        
        val filtered = state.allAthletes.filter { athlete ->
            val matchesQuery = if (query.isBlank()) true else athlete.fullName.contains(query, ignoreCase = true)
            val matchesSex = if (state.sexFilter == null) true else athlete.sex == state.sexFilter
            matchesQuery && matchesSex
        }.sortedBy { it.lastName }

        _uiState.update { it.copy(filteredAthletes = filtered) }
    }
}
