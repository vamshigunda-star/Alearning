package com.vamshi.field.ui.athlete

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vamshi.field.domain.model.reports.AthleteTestDetailData
import com.vamshi.field.domain.model.reports.AttemptRow
import com.vamshi.field.domain.repository.TestingRepository
import com.vamshi.field.domain.usecase.reports.ObserveAthleteTestDetailUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AthleteTestDetailUiState(
    val data: AthleteTestDetailData? = null,
    val showPeerSheet: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isDeleting: Boolean = false,
    val deleteCandidate: AttemptRow? = null
)

sealed interface AthleteTestDetailAction {
    data object OnOpenPeerSheet : AthleteTestDetailAction
    data object OnDismissPeerSheet : AthleteTestDetailAction
    data object OnNavigateBack : AthleteTestDetailAction
    data class OnRequestDelete(val attempt: AttemptRow) : AthleteTestDetailAction
    data object OnConfirmDelete : AthleteTestDetailAction
    data object OnDismissDelete : AthleteTestDetailAction
    data object OnDismissError : AthleteTestDetailAction
}

@HiltViewModel
class AthleteTestDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeAthleteTestDetail: ObserveAthleteTestDetailUseCase,
    private val testingRepository: TestingRepository
) : ViewModel() {

    var athleteId: String = ""
        private set
    var testId: String = ""
        private set
    var contextSessionId: String? = null
        private set

    private val _uiState = MutableStateFlow(AthleteTestDetailUiState())
    val uiState: StateFlow<AthleteTestDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        athleteId = savedStateHandle.get<String>("athleteId") ?: ""
        testId = savedStateHandle.get<String>("testId") ?: ""
        contextSessionId = savedStateHandle.get<String>("contextSessionId")
        if (athleteId.isNotBlank() && testId.isNotBlank()) {
            loadDetail(athleteId, testId, contextSessionId)
        }
    }

    private fun loadDetail(athleteId: String, testId: String, contextSessionId: String?) {
        this.athleteId = athleteId
        this.testId = testId
        this.contextSessionId = contextSessionId
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            observeAthleteTestDetail(athleteId, testId, contextSessionId)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
                .collect { d -> _uiState.update { it.copy(data = d, isLoading = false) } }
        }
    }

    fun onAction(action: AthleteTestDetailAction) {
        when (action) {
            AthleteTestDetailAction.OnOpenPeerSheet -> _uiState.update { it.copy(showPeerSheet = true) }
            AthleteTestDetailAction.OnDismissPeerSheet -> _uiState.update { it.copy(showPeerSheet = false) }
            AthleteTestDetailAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            is AthleteTestDetailAction.OnRequestDelete -> _uiState.update { it.copy(deleteCandidate = action.attempt) }
            AthleteTestDetailAction.OnDismissDelete -> _uiState.update { it.copy(deleteCandidate = null) }
            AthleteTestDetailAction.OnConfirmDelete -> confirmDelete()
            AthleteTestDetailAction.OnNavigateBack -> Unit
        }
    }

    private fun confirmDelete() {
        val candidate = _uiState.value.deleteCandidate ?: return
        _uiState.update { it.copy(isDeleting = true, deleteCandidate = null) }
        viewModelScope.launch {
            try {
                testingRepository.deleteResultById(candidate.resultId)
                _uiState.update { it.copy(isDeleting = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Delete failed: ${e.message}", isDeleting = false) }
            }
        }
    }
}

