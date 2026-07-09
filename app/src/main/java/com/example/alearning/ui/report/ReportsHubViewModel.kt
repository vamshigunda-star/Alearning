package com.example.alearning.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.reports.AthleteDashboardData
import com.example.alearning.domain.model.reports.ReportsHomeData
import com.example.alearning.domain.model.reports.SessionReportData
import com.example.alearning.domain.repository.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UI State ──────────────────────────────────────────────────────────────────

data class ReportsHubUiState(
    val homeData: ReportsHomeData? = null,
    val isLoadingHome: Boolean = true,

    // Athlete Profile tab
    val selectedAthleteId: String? = null,
    val athleteData: AthleteDashboardData? = null,
    val isLoadingAthlete: Boolean = false,
    val selectedAthleteTestId: String? = null,

    // Event Report tab
    val selectedEventId: String? = null,
    val selectedEventGroupId: String? = null,
    val eventData: SessionReportData? = null,
    val isLoadingEvent: Boolean = false,
    val selectedEventTestId: String? = null,

    val errorMessage: String? = null
)

// ── Actions ───────────────────────────────────────────────────────────────────

sealed interface ReportsHubAction {
    data class SelectAthlete(val id: String) : ReportsHubAction
    data class SelectAthleteTest(val testId: String) : ReportsHubAction
    data class SelectEvent(val eventId: String, val groupId: String) : ReportsHubAction
    data class SelectEventTest(val testId: String) : ReportsHubAction
    data object DismissError : ReportsHubAction
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class ReportsHubViewModel @Inject constructor(
    private val reports: ReportsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsHubUiState())
    val uiState: StateFlow<ReportsHubUiState> = _uiState.asStateFlow()

    private var athleteJob: Job? = null
    private var eventJob: Job? = null

    init {
        viewModelScope.launch {
            reports.observeHome()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoadingHome = false) } }
                .collect { data ->
                    _uiState.update { it.copy(homeData = data, isLoadingHome = false) }

                    // Auto-load first athlete from flags list if nothing selected yet
                    if (_uiState.value.selectedAthleteId == null) {
                        val firstId = data.flags.firstOrNull()?.individualId
                        if (firstId != null) loadAthlete(firstId)
                    }

                    // Auto-load first event session if nothing selected yet
                    if (_uiState.value.selectedEventId == null) {
                        val first = data.recentSessions.firstOrNull()
                        if (first != null && first.groupId != null) {
                            loadEvent(first.event.id, first.groupId)
                        }
                    }
                }
        }
    }

    fun onAction(action: ReportsHubAction) {
        when (action) {
            is ReportsHubAction.SelectAthlete -> loadAthlete(action.id)
            is ReportsHubAction.SelectAthleteTest -> _uiState.update { it.copy(selectedAthleteTestId = action.testId) }
            is ReportsHubAction.SelectEvent -> loadEvent(action.eventId, action.groupId)
            is ReportsHubAction.SelectEventTest -> _uiState.update { it.copy(selectedEventTestId = action.testId) }
            ReportsHubAction.DismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun loadAthlete(id: String) {
        athleteJob?.cancel()
        _uiState.update { it.copy(selectedAthleteId = id, isLoadingAthlete = true, athleteData = null, selectedAthleteTestId = null) }
        athleteJob = viewModelScope.launch {
            reports.observeAthleteDashboard(id, null)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoadingAthlete = false) } }
                .collect { data ->
                    _uiState.update { state ->
                        state.copy(
                            athleteData = data,
                            isLoadingAthlete = false,
                            selectedAthleteTestId = state.selectedAthleteTestId ?: data?.tiles?.firstOrNull()?.test?.id
                        )
                    }
                }
        }
    }

    private fun loadEvent(eventId: String, groupId: String) {
        eventJob?.cancel()
        _uiState.update {
            it.copy(
                selectedEventId = eventId,
                selectedEventGroupId = groupId,
                isLoadingEvent = true,
                eventData = null,
                selectedEventTestId = null
            )
        }
        eventJob = viewModelScope.launch {
            reports.observeSessionReport(groupId, eventId)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoadingEvent = false) } }
                .collect { data ->
                    _uiState.update { state ->
                        state.copy(
                            eventData = data,
                            isLoadingEvent = false,
                            selectedEventTestId = state.selectedEventTestId ?: data?.tests?.firstOrNull()?.id
                        )
                    }
                }
        }
    }
}
