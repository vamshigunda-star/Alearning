package com.example.alearning.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.analytics.AnalyticsAction
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.domain.usecase.analytics.GetIndividualAnalyticsUseCase
import com.example.alearning.domain.usecase.analytics.IndividualAnalyticsSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val availableIndividuals: List<Individual> = emptyList(),
    val selectedIndividualId: String? = null,
    val individualAnalytics: IndividualAnalyticsSnapshot? = null,
    val isAIGenerating: Boolean = false,
    val aiPrescription: String? = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val testingRepository: TestingRepository,
    private val standardsRepository: StandardsRepository,
    private val getIndividualAnalyticsUseCase: GetIndividualAnalyticsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        observeIndividuals()
    }

    private fun observeIndividuals() {
        viewModelScope.launch {
            peopleRepository.getAllIndividuals()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { individuals ->
                    val previouslySelected = _uiState.value.selectedIndividualId
                    val newSelected = previouslySelected ?: individuals.firstOrNull()?.id
                    
                    _uiState.update { 
                        it.copy(
                            availableIndividuals = individuals,
                            selectedIndividualId = newSelected,
                            isLoading = false
                        )
                    }
                    if (newSelected != null) {
                        recomputeIndividualAnalytics()
                    }
                }
        }
    }

    fun onAction(action: AnalyticsAction) {
        when (action) {
            is AnalyticsAction.SelectIndividual -> {
                _uiState.update { it.copy(selectedIndividualId = action.individualId, aiPrescription = null) }
                recomputeIndividualAnalytics()
            }
            is AnalyticsAction.GenerateAIPrescription -> {
                _uiState.update { it.copy(isAIGenerating = true) }
                viewModelScope.launch {
                    kotlinx.coroutines.delay(1500)
                    _uiState.update { 
                        it.copy(
                            isAIGenerating = false,
                            aiPrescription = "Mock AI Prescription: Increase load by 5% and focus on concentric phase to overcome the recent plateau."
                        )
                    }
                }
            }
        }
    }

    private fun recomputeIndividualAnalytics() {
        val athleteId = _uiState.value.selectedIndividualId ?: return
        viewModelScope.launch {
            try {
                val results = testingRepository.getAllResults().first()
                val categories = standardsRepository.getAllCategories().first()
                val tests = standardsRepository.getAllTests().first()
                
                val snapshot = getIndividualAnalyticsUseCase(athleteId, results, categories, tests)
                _uiState.update { it.copy(individualAnalytics = snapshot) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message) }
            }
        }
    }
}
