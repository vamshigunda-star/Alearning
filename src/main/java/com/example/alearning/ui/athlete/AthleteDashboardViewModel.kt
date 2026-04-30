package com.example.alearning.ui.athlete

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.reports.AthleteDashboardData
import com.example.alearning.reports.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.domain.usecase.testing.AthleteRadarData
import com.example.alearning.domain.usecase.testing.GetAthleteRadarDataUseCase
import kotlinx.coroutines.flow.first

data class AthleteDashboardUiState(
    val data: AthleteDashboardData? = null,
    val radarData: AthleteRadarData? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isExporting: Boolean = false
)

sealed interface AthleteDashboardAction {
    data class OnNavigateToTest(val testId: String) : AthleteDashboardAction
    data class OnStartQuickTest(val testIds: List<String>) : AthleteDashboardAction
    data object OnNavigateBack : AthleteDashboardAction
    data object OnExportCsv : AthleteDashboardAction
    data object OnDismissError : AthleteDashboardAction
}

@HiltViewModel
class AthleteDashboardViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reports: ReportsRepository,
    private val testingRepository: TestingRepository,
    private val standardsRepository: StandardsRepository,
    private val getAthleteRadarData: GetAthleteRadarDataUseCase
) : ViewModel() {

    val athleteId: String = savedStateHandle["athleteId"] ?: ""
    val contextSessionId: String? = savedStateHandle["contextSessionId"]

    private val _uiState = MutableStateFlow(AthleteDashboardUiState())
    val uiState: StateFlow<AthleteDashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reports.observeAthleteDashboard(athleteId, contextSessionId)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
                .collect { d ->
                    _uiState.update { it.copy(data = d, isLoading = false) }
                    // Recompute radar whenever the dashboard data emits (results changed).
                    if (d != null) {
                        try {
                            val radar = getAthleteRadarData(athleteId)
                            _uiState.update { it.copy(radarData = radar) }
                        } catch (_: Exception) {
                            // Radar is non-essential; ignore failures so the screen still loads.
                        }
                    }
                }
        }
    }

    fun onAction(action: AthleteDashboardAction) {
        when (action) {
            AthleteDashboardAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            AthleteDashboardAction.OnExportCsv -> exportResults()
            else -> Unit
        }
    }

    private fun exportResults() {
        val athlete = _uiState.value.data?.athlete ?: return
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            try {
                val results = testingRepository.getAllResultsForIndividual(athleteId).first()
                val tests = standardsRepository.getAllTests().first().associateBy { it.id }
                _uiState.update { it.copy(isExporting = false) }
                // We'll trigger the actual export from the UI to get the context
                _exportEvent.emit(ExportRequest.Athlete(athlete, results, tests))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Export failed: ${e.message}", isExporting = false) }
            }
        }
    }

    private val _exportEvent = kotlinx.coroutines.flow.MutableSharedFlow<ExportRequest>()
    val exportEvent = _exportEvent.asSharedFlow()

    sealed interface ExportRequest {
        data class Athlete(
            val athlete: Individual,
            val results: List<com.example.alearning.domain.model.testing.TestResult>,
            val tests: Map<String, com.example.alearning.domain.model.standards.FitnessTest>
        ) : ExportRequest
    }
}

