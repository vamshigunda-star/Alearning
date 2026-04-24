package com.example.alearning.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.standards.RadarAxis
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.domain.usecase.testing.AthleteRadarData
import com.example.alearning.domain.usecase.testing.RadarAxisScore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyticsUiState(
    val rosterRadarData: AthleteRadarData? = null,
    val scoreDistribution: Map<String, Int> = emptyMap(), // Classification to count
    val totalAthletes: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val testingRepository: TestingRepository,
    private val standardsRepository: StandardsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadGlobalAnalytics()
    }

    private fun loadGlobalAnalytics() {
        viewModelScope.launch {
            try {
                val athletes = peopleRepository.getAllIndividuals().first()
                if (athletes.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, totalAthletes = 0) }
                    return@launch
                }

                // Global Radar Data (Average of all athletes)
                val allRadarData = mutableMapOf<RadarAxis, MutableList<Int>>()
                val distributions = mutableMapOf<String, Int>()
                
                for (athlete in athletes) {
                    val results = testingRepository.getLatestResultPerTestForIndividual(athlete.id)
                    for (result in results) {
                        // Distribution
                        result.classification?.let { cls ->
                            distributions[cls] = (distributions[cls] ?: 0) + 1
                        }

                        // Radar
                        val test = standardsRepository.getTestById(result.testId) ?: continue
                        val category = standardsRepository.getAllCategories().first().find { it.id == test.categoryId }
                        val axis = category?.radarAxis ?: continue
                        val percentile = result.percentile ?: continue
                        allRadarData.getOrPut(axis) { mutableListOf() }.add(percentile)
                    }
                }

                val globalAxisScores = allRadarData.map { (axis, percentiles) ->
                    RadarAxisScore(
                        axis = axis,
                        normalizedScore = percentiles.average().toFloat() / 100f,
                        testCount = percentiles.size
                    )
                }

                _uiState.update { it.copy(
                    rosterRadarData = AthleteRadarData("global", globalAxisScores),
                    scoreDistribution = distributions,
                    totalAthletes = athletes.size,
                    isLoading = false
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }
}
