package com.example.alearning.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.Group
import com.example.alearning.domain.model.testing.TestingEvent
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.domain.usecase.auth.ObserveCurrentUserUseCase
import com.example.alearning.domain.usecase.auth.SignOutUseCase
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
    val activeAthletes: Int = 0,
    val scheduledTestCount: Int = 0,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    /** Coach's first name for the greeting; defaults to empty (shows "Coach"). */
    val coachFirstName: String = "",
    /** Coach's last name; included so the greeting can use a full name if desired. */
    val coachLastName: String = "",
    /** True after sign-out; screen navigates to SignIn via LaunchedEffect. */
    val navigateToSignIn: Boolean = false
)

sealed interface DashboardAction {
    data object OnCreateEventClick : DashboardAction
    data object OnQuickTestClick : DashboardAction
    data object OnRosterClick : DashboardAction
    data object OnTestLibraryClick : DashboardAction
    data class OnEventClick(val eventId: String, val groupId: String) : DashboardAction
    data object OnDismissError : DashboardAction
    data object OnLeaderboardClick : DashboardAction
    data object OnAnalyticsClick : DashboardAction
    data object OnSeeAllEventsClick : DashboardAction
    data object OnSignOutClick : DashboardAction
    data object NavigationConsumed : DashboardAction
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val testingRepository: TestingRepository,
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val signOutUseCase: SignOutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
        observeCoachName()
    }

    fun onAction(action: DashboardAction) {
        when (action) {
            is DashboardAction.OnDismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
            DashboardAction.OnSignOutClick -> {
                viewModelScope.launch {
                    signOutUseCase()
                    _uiState.update { it.copy(navigateToSignIn = true) }
                }
            }
            DashboardAction.NavigationConsumed -> {
                _uiState.update { it.copy(navigateToSignIn = false) }
            }
            else -> Unit
        }
    }

    private fun observeCoachName() {
        viewModelScope.launch {
            observeCurrentUser()
                .catch { /* non-critical — greeting degrades to "Coach" */ }
                .collect { user ->
                    _uiState.update {
                        it.copy(
                            coachFirstName = user?.firstName ?: "",
                            coachLastName = user?.lastName ?: ""
                        )
                    }
                }
        }
    }

    private fun loadDashboardData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            peopleRepository.getAllIndividuals()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { athletes ->
                    _uiState.update { it.copy(activeAthletes = athletes.size) }
                }
        }
        viewModelScope.launch {
            peopleRepository.getAllGroups()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { groups ->
                    _uiState.update { it.copy(groups = groups) }
                }
        }
        viewModelScope.launch {
            testingRepository.getAllEvents()
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = e.message, isLoading = false) }
                }
                .collect { events ->
                    _uiState.update {
                        it.copy(
                            recentEvents = events.sortedByDescending { it.date }.take(5),
                            scheduledTestCount = events.size,
                            isLoading = false
                        )
                    }
                }
        }
    }
}
