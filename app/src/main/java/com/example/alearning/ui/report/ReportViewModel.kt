package com.example.alearning.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.Group
import com.example.alearning.domain.model.testing.TestingEvent
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.TestingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val groups: List<Group> = emptyList(),
    val recentEvents: List<TestingEvent> = emptyList(),
    val needsAttentionCount: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface ReportAction {
    data object OnDismissError : ReportAction
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val testingRepository: TestingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                peopleRepository.getAllGroups(),
                testingRepository.getAllEvents()
            ) { groups: List<Group>, events: List<TestingEvent> ->
                // Simple placeholder for attention count for landing page performance
                val attentionCount = 0
                
                Triple(groups, events, attentionCount)
            }.catch { e ->
                _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
            }.collect { (groups, events, count) ->
                _uiState.update { it.copy(
                    groups = groups,
                    recentEvents = events,
                    needsAttentionCount = count,
                    isLoading = false
                ) }
            }
        }
    }

    fun onAction(action: ReportAction) {
        when (action) {
            ReportAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
        }
    }
}
