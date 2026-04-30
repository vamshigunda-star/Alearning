package com.example.alearning.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.analytics.*
import com.example.alearning.domain.model.standards.RadarAxis
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.domain.usecase.analytics.GenerateInsightsUseCase
import com.example.alearning.domain.usecase.testing.AthleteRadarData
import com.example.alearning.domain.usecase.testing.GetGroupProgressUseCase
import com.example.alearning.domain.usecase.testing.GetRemediationListUseCase
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
    // --- shared ---
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedPeriod: TimePeriod = TimePeriod.LAST_6_MONTHS,

    // --- Tab 0: Overview ---
    val rosterRadarData: AthleteRadarData? = null,
    val scoreDistribution: Map<String, Int> = emptyMap(),
    val totalAthletes: Int = 0,
    val dataCompleteness: Float = 0f,
    val strongestDomain: String? = null,
    val weakestDomain: String? = null,

    // --- Tab 1: Trends ---
    val groupProgressData: Map<String, List<GroupProgressPoint>> = emptyMap(),
    val selectedDomain: String? = null,
    val availableGroups: List<String> = emptyList(),
    val selectedGroups: Set<String> = emptySet(),

    // --- Tab 2: Remediation ---
    val atRiskAthletes: List<AtRiskAthlete> = emptyList(),
    val domainPatterns: List<DomainPattern> = emptyList(),
    val riskThreshold: Float = 0.4f,
    val remediationCount: Int = 0,

    // --- Tab 3: Insights ---
    val priorityInsights: List<PriorityInsight> = emptyList(),
    val topMovers: List<AthleteMover> = emptyList(),
    val recommendedActions: List<RecommendedAction> = emptyList(),
    val strengthsToLeverage: List<String> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val testingRepository: TestingRepository,
    private val standardsRepository: StandardsRepository,
    private val getGroupProgressUseCase: GetGroupProgressUseCase,
    private val getRemediationListUseCase: GetRemediationListUseCase,
    private val generateInsightsUseCase: GenerateInsightsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState(isLoading = true))
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    // Keep all global un-filtered lists
    private var allAtRiskAthletes: List<AtRiskAthlete> = emptyList()
    private var allTrendData: Map<String, List<GroupProgressPoint>> = emptyMap()

    init {
        loadGlobalAnalytics()
        loadGroupProgress()
    }

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun updatePeriod(period: TimePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadGroupProgress()
    }

    fun updateDomainFilter(domain: String?) {
        _uiState.update { it.copy(selectedDomain = domain) }
        filterTrendData()
    }

    fun toggleGroup(group: String) {
        _uiState.update {
            val updatedGroups = it.selectedGroups.toMutableSet()
            if (updatedGroups.contains(group)) {
                updatedGroups.remove(group)
            } else {
                updatedGroups.add(group)
            }
            it.copy(selectedGroups = updatedGroups)
        }
        filterTrendData()
    }

    fun updateThreshold(threshold: Float) {
        _uiState.update { it.copy(riskThreshold = threshold) }
        filterAtRiskAthletes()
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
                var testedAthletesCount = 0

                for (athlete in athletes) {
                    val results = testingRepository.getLatestResultPerTestForIndividual(athlete.id)
                    if (results.isNotEmpty()) {
                        testedAthletesCount++
                    }

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

                val dataCompleteness = if (athletes.isNotEmpty()) {
                    testedAthletesCount.toFloat() / athletes.size
                } else 0f

                val sortedAxes = globalAxisScores.sortedByDescending { it.normalizedScore }
                val strongestDomain = sortedAxes.firstOrNull()?.axis?.name
                val weakestDomain = sortedAxes.lastOrNull()?.axis?.name
                
                val rosterRadarData = AthleteRadarData("global", globalAxisScores)

                // Remediation List
                allAtRiskAthletes = getRemediationListUseCase.getGlobalAtRiskAthletes()
                
                _uiState.update {
                    it.copy(
                        rosterRadarData = rosterRadarData,
                        scoreDistribution = distributions,
                        totalAthletes = athletes.size,
                        dataCompleteness = dataCompleteness,
                        strongestDomain = strongestDomain,
                        weakestDomain = weakestDomain,
                    )
                }
                
                filterAtRiskAthletes()
                generateInsights()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }

    private fun loadGroupProgress() {
        viewModelScope.launch {
            try {
                val period = _uiState.value.selectedPeriod
                allTrendData = getGroupProgressUseCase(period)
                
                val allGroups = peopleRepository.getAllGroups().first().map { it.name }
                
                _uiState.update { state -> 
                    val selectedGroups = state.selectedGroups.ifEmpty { allGroups.toSet() }
                    state.copy(
                        availableGroups = allGroups,
                        selectedGroups = selectedGroups
                    )
                }
                filterTrendData()
                generateInsights()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }
        }
    }

    private fun filterTrendData() {
        val state = _uiState.value
        val selectedGroups = state.selectedGroups

        val filteredData = allTrendData.filterKeys { it in selectedGroups }

        _uiState.update { it.copy(groupProgressData = filteredData) }
    }

    private fun filterAtRiskAthletes() {
        val threshold = _uiState.value.riskThreshold
        val filtered = allAtRiskAthletes.filter { it.avgScore < threshold }
        val patterns = getRemediationListUseCase.calculateDomainPatterns(filtered)
        
        _uiState.update { 
            it.copy(
                atRiskAthletes = filtered.sortedBy { a -> a.avgScore },
                domainPatterns = patterns,
                remediationCount = filtered.size,
                isLoading = false // Ensure we clear loading once this finishes
            )
        }
    }
    
    private fun generateInsights() {
        val state = _uiState.value
        val insightsData = generateInsightsUseCase.invoke(
            rosterRadarData = state.rosterRadarData,
            atRiskAthletes = state.atRiskAthletes, // using the filtered one
            trendData = state.groupProgressData
        )

        val strengths = state.rosterRadarData?.axisScores
            ?.asSequence()
            ?.filter { it.normalizedScore > 0.7f }
            ?.map { it.axis.name }
            ?.toList() ?: emptyList()

        _uiState.update {
            it.copy(
                priorityInsights = insightsData.first,
                topMovers = insightsData.second,
                recommendedActions = insightsData.third,
                strengthsToLeverage = strengths
            )
        }
    }
}