package com.example.alearning.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.Group
import com.example.alearning.domain.model.testing.TestingEvent
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.TestingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val recentEvents: List<TestingEvent> = emptyList(),
    val groups: List<Group> = emptyList(),
    val athleteCount: Int = 0,
    val groupCount: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface DashboardAction {
    data object OnCreateEventClick : DashboardAction
    data object OnQuickTestClick : DashboardAction
    data object OnRosterClick : DashboardAction
    data object OnTestLibraryClick : DashboardAction
    data class OnEventClick(val eventId: String, val groupId: String) : DashboardAction
    data object OnDismissError : DashboardAction
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val testingRepository: TestingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun onAction(action: DashboardAction) {
        when (action) {
            is DashboardAction.OnDismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
            // Navigation actions handled by the screen composable
            else -> Unit
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            peopleRepository.getAllIndividuals()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
                }
                .collect { athletes ->
                    _uiState.update { it.copy(athleteCount = athletes.size, isLoading = false) }
                }
        }
        viewModelScope.launch {
            peopleRepository.getAllGroups()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
                }
                .collect { groups ->
                    _uiState.update { it.copy(groups = groups, groupCount = groups.size) }
                }
        }
        viewModelScope.launch {
            testingRepository.getAllEvents()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
                }
                .collect { events ->
                    _uiState.update { it.copy(recentEvents = events.take(5)) }
                }
        }
    }
}
