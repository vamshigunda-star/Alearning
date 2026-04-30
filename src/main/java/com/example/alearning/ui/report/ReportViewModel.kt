package com.example.alearning.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.reports.ReportsHomeData
import com.example.alearning.reports.ReportsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val data: ReportsHomeData? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface ReportAction {
    data class OnNavigateToGroup(val groupId: String) : ReportAction
    data class OnNavigateToSession(val groupId: String, val sessionId: String) : ReportAction
    data class OnNavigateToAthlete(val individualId: String, val sessionId: String?) : ReportAction
    data object OnDismissError : ReportAction
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reports: ReportsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            reports.observeHome()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
                .collect { d -> _uiState.update { it.copy(data = d, isLoading = false) } }
        }
    }

    fun onAction(action: ReportAction) {
        when (action) {
            ReportAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            else -> Unit
        }
    }
}
