package com.example.alearning.ui.athlete

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.reports.AthleteTestDetailData
import com.example.alearning.reports.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.reports.AttemptRow

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
    private val reports: ReportsRepository,
    private val testingRepository: TestingRepository
) : ViewModel() {

    val athleteId: String = savedStateHandle["athleteId"] ?: ""
    val testId: String = savedStateHandle["testId"] ?: ""
    val contextSessionId: String? = savedStateHandle["contextSessionId"]

    private val _uiState = MutableStateFlow(AthleteTestDetailUiState())
    val uiState: StateFlow<AthleteTestDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reports.observeAthleteTestDetail(athleteId, testId, contextSessionId)
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

