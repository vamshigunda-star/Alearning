package com.example.alearning.ui.session

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.reports.ReportsRepository
import com.example.alearning.reports.SessionReportData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.domain.repository.StandardsRepository
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first

data class SessionReportUiState(
    val data: SessionReportData? = null,
    val selectedTestId: String? = null,
    val isSwitcherOpen: Boolean = false,
    val isLoading: Boolean = true,
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteDialog: Boolean = false,
    val isDeleted: Boolean = false
)

sealed interface SessionReportAction {
    data class OnSelectTest(val testId: String) : SessionReportAction
    data class OnSwitchSession(val sessionId: String) : SessionReportAction
    data object OnOpenSwitcher : SessionReportAction
    data object OnDismissSwitcher : SessionReportAction
    data class OnNavigateToAthlete(val individualId: String) : SessionReportAction
    data object OnResumeTesting : SessionReportAction
    data object OnNavigateBack : SessionReportAction
    data object OnExportCsv : SessionReportAction
    data object OnDismissError : SessionReportAction
    data object OnRequestDelete : SessionReportAction
    data object OnConfirmDelete : SessionReportAction
    data object OnDismissDelete : SessionReportAction
}

@HiltViewModel
class SessionReportViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val reports: ReportsRepository,
    private val testingRepository: TestingRepository,
    private val peopleRepository: PeopleRepository,
    private val standardsRepository: StandardsRepository
) : ViewModel() {

    val groupId: String = savedStateHandle["groupId"] ?: ""
    private val initialSessionId: String = savedStateHandle["sessionId"] ?: ""

    private val _uiState = MutableStateFlow(SessionReportUiState())
    val uiState: StateFlow<SessionReportUiState> = _uiState.asStateFlow()

    private val _exportEvent = kotlinx.coroutines.flow.MutableSharedFlow<ExportRequest>()
    val exportEvent = _exportEvent.asSharedFlow()

    sealed interface ExportRequest {
        data class Event(
            val eventName: String,
            val results: List<Pair<com.example.alearning.domain.model.people.Individual, com.example.alearning.domain.model.testing.TestResult>>,
            val tests: Map<String, com.example.alearning.domain.model.standards.FitnessTest>
        ) : ExportRequest
    }

    init {
        load(initialSessionId)
    }

    fun onAction(action: SessionReportAction) {
        when (action) {
            is SessionReportAction.OnSelectTest -> _uiState.update { it.copy(selectedTestId = action.testId) }
            SessionReportAction.OnOpenSwitcher -> _uiState.update { it.copy(isSwitcherOpen = true) }
            SessionReportAction.OnDismissSwitcher -> _uiState.update { it.copy(isSwitcherOpen = false) }
            is SessionReportAction.OnSwitchSession -> {
                _uiState.update { it.copy(isSwitcherOpen = false, isLoading = true, data = null) }
                load(action.sessionId)
            }
            SessionReportAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            SessionReportAction.OnExportCsv -> exportResults()
            SessionReportAction.OnRequestDelete -> _uiState.update { it.copy(showDeleteDialog = true) }
            SessionReportAction.OnDismissDelete -> _uiState.update { it.copy(showDeleteDialog = false) }
            SessionReportAction.OnConfirmDelete -> deleteEvent()
            is SessionReportAction.OnNavigateToAthlete,
            SessionReportAction.OnResumeTesting,
            SessionReportAction.OnNavigateBack -> Unit
        }
    }

    private fun deleteEvent() {
        val eventId = _uiState.value.data?.event?.id ?: return
        viewModelScope.launch {
            try {
                testingRepository.deleteEventById(eventId)
                _uiState.update { it.copy(showDeleteDialog = false, isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(showDeleteDialog = false, errorMessage = "Failed to delete: ${e.message}")
                }
            }
        }
    }

    private fun load(sessionId: String) {
        viewModelScope.launch {
            reports.observeSessionReport(groupId, sessionId)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
                .collect { data ->
                    _uiState.update {
                        it.copy(
                            data = data,
                            selectedTestId = it.selectedTestId ?: data?.tests?.firstOrNull()?.id,
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun exportResults() {
        val data = _uiState.value.data ?: return
        _uiState.update { it.copy(isExporting = true) }
        viewModelScope.launch {
            try {
                val results = testingRepository.getEventResults(data.event.id).first()
                val athleteIds = results.map { it.individualId }.distinct()
                val athletesMap = peopleRepository.getIndividualsByIds(athleteIds).first().associateBy { it.id }
                val tests = standardsRepository.getAllTests().first().associateBy { it.id }
                
                val pairedResults = results.mapNotNull { result ->
                    athletesMap[result.individualId]?.let { it to result }
                }
                
                _uiState.update { it.copy(isExporting = false) }
                _exportEvent.emit(ExportRequest.Event(data.event.name, pairedResults, tests))
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Export failed: ${e.message}", isExporting = false) }
            }
        }
    }
}

