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
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val recentEvents: List<TestingEvent> = emptyList(),
    val groups: List<Group> = emptyList(),
    val studentCount: Int = 0,
    val groupCount: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val testingRepository: TestingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            peopleRepository.getAllIndividuals().collect { students ->
                _uiState.value = _uiState.value.copy(studentCount = students.size)
            }
        }
        viewModelScope.launch {
            peopleRepository.getAllGroups().collect { groups ->
                _uiState.value = _uiState.value.copy(
                    groups = groups,
                    groupCount = groups.size
                )
            }
        }
        viewModelScope.launch {
            testingRepository.getAllEvents().collect { events ->
                _uiState.value = _uiState.value.copy(recentEvents = events.take(5))
            }
        }
    }
}
