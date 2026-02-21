package com.example.alearning.ui.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.usecase.testing.AthleteProfile
import com.example.alearning.domain.usecase.testing.GetAthleteProfileUseCase
import com.example.alearning.domain.usecase.testing.GetIndividualProgressUseCase
import com.example.alearning.domain.usecase.testing.IndividualProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AthleteReportUiState(
    val athlete: Individual? = null,
    val profile: AthleteProfile? = null,
    val selectedTestProgress: IndividualProgress? = null,
    val selectedTestId: String? = null,
    val isLoading: Boolean = true,
    val isProgressLoading: Boolean = false,
    val errorMessage: String? = null
)

sealed interface AthleteReportAction {
    data class OnSelectTest(val testId: String) : AthleteReportAction
    data object OnNavigateBack : AthleteReportAction
    data object OnDismissError : AthleteReportAction
}

@HiltViewModel
class AthleteReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val peopleRepository: PeopleRepository,
    private val getAthleteProfile: GetAthleteProfileUseCase,
    private val getIndividualProgress: GetIndividualProgressUseCase
) : ViewModel() {

    private val individualId: String = savedStateHandle["individualId"] ?: ""

    private val _uiState = MutableStateFlow(AthleteReportUiState())
    val uiState: StateFlow<AthleteReportUiState> = _uiState.asStateFlow()

    init {
        loadAthleteData()
    }

    fun onAction(action: AthleteReportAction) {
        when (action) {
            is AthleteReportAction.OnSelectTest -> loadProgress(action.testId)
            is AthleteReportAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            is AthleteReportAction.OnNavigateBack -> Unit
        }
    }

    private fun loadAthleteData() {
        viewModelScope.launch {
            try {
                val athlete = peopleRepository.getIndividualById(individualId)
                _uiState.update { it.copy(athlete = athlete) }

                val profile = getAthleteProfile(individualId)
                _uiState.update { it.copy(profile = profile, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }

    private fun loadProgress(testId: String) {
        _uiState.update { it.copy(selectedTestId = testId, isProgressLoading = true, selectedTestProgress = null) }
        viewModelScope.launch {
            try {
                val progress = getIndividualProgress(individualId, testId)
                _uiState.update { it.copy(selectedTestProgress = progress, isProgressLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isProgressLoading = false) }
            }
        }
    }
}
