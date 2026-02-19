package com.example.alearning.ui.roster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.model.people.Group
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.usecase.people.CreateGroupUseCase
import com.example.alearning.domain.usecase.people.ManageRosterUseCase
import com.example.alearning.domain.usecase.people.RegisterAthleteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RosterUiState(
    val groups: List<Group> = emptyList(),
    val selectedGroup: Group? = null,
    val athletesInGroup: List<Individual> = emptyList(),
    val allAthletes: List<Individual> = emptyList(),
    val showAddGroupDialog: Boolean = false,
    val showRegisterAthleteDialog: Boolean = false,
    val showAddToGroupDialog: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed interface RosterAction {
    data class OnSelectGroup(val group: Group) : RosterAction
    data object OnShowAddGroupDialog : RosterAction
    data object OnDismissAddGroupDialog : RosterAction
    data class OnCreateGroup(val name: String, val location: String?, val cycle: String?) : RosterAction
    data object OnShowRegisterAthleteDialog : RosterAction
    data object OnDismissRegisterAthleteDialog : RosterAction
    data class OnRegisterAthlete(
        val firstName: String,
        val lastName: String,
        val dateOfBirth: Long,
        val sex: BiologicalSex,
        val email: String?,
        val medicalAlert: String?
    ) : RosterAction
    data object OnShowAddToGroupDialog : RosterAction
    data object OnDismissAddToGroupDialog : RosterAction
    data class OnAddAthleteToGroup(val individualId: String) : RosterAction
    data class OnRemoveAthleteFromGroup(val individualId: String) : RosterAction
    data class OnNavigateToAthleteReport(val individualId: String) : RosterAction
    data object OnNavigateBack : RosterAction
    data object OnDismissError : RosterAction
}

@HiltViewModel
class RosterViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val registerAthlete: RegisterAthleteUseCase,
    private val createGroup: CreateGroupUseCase,
    private val manageRoster: ManageRosterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RosterUiState())
    val uiState: StateFlow<RosterUiState> = _uiState.asStateFlow()

    private var groupMembersJob: Job? = null

    init {
        viewModelScope.launch {
            peopleRepository.getAllGroups()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message, isLoading = false) } }
                .collect { groups ->
                    _uiState.update { current ->
                        val selected = current.selectedGroup
                            ?: groups.firstOrNull()
                        current.copy(groups = groups, selectedGroup = selected, isLoading = false)
                    }
                    // Load members for auto-selected group
                    val selected = _uiState.value.selectedGroup
                    if (selected != null && groupMembersJob == null) {
                        loadGroupMembers(selected.id)
                    }
                }
        }
        viewModelScope.launch {
            peopleRepository.getAllIndividuals()
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { athletes ->
                    _uiState.update { it.copy(allAthletes = athletes) }
                }
        }
    }

    fun onAction(action: RosterAction) {
        when (action) {
            is RosterAction.OnSelectGroup -> selectGroup(action.group)
            is RosterAction.OnShowAddGroupDialog -> _uiState.update { it.copy(showAddGroupDialog = true) }
            is RosterAction.OnDismissAddGroupDialog -> _uiState.update { it.copy(showAddGroupDialog = false) }
            is RosterAction.OnCreateGroup -> addGroup(action.name, action.location, action.cycle)
            is RosterAction.OnShowRegisterAthleteDialog -> _uiState.update { it.copy(showRegisterAthleteDialog = true) }
            is RosterAction.OnDismissRegisterAthleteDialog -> _uiState.update { it.copy(showRegisterAthleteDialog = false) }
            is RosterAction.OnRegisterAthlete -> registerNewAthlete(action)
            is RosterAction.OnShowAddToGroupDialog -> _uiState.update { it.copy(showAddToGroupDialog = true) }
            is RosterAction.OnDismissAddToGroupDialog -> _uiState.update { it.copy(showAddToGroupDialog = false) }
            is RosterAction.OnAddAthleteToGroup -> addAthleteToGroup(action.individualId)
            is RosterAction.OnRemoveAthleteFromGroup -> removeAthleteFromGroup(action.individualId)
            is RosterAction.OnDismissError -> _uiState.update { it.copy(errorMessage = null) }
            // Navigation actions handled by the screen composable
            is RosterAction.OnNavigateToAthleteReport -> Unit
            is RosterAction.OnNavigateBack -> Unit
        }
    }

    private fun selectGroup(group: Group) {
        _uiState.update { it.copy(selectedGroup = group) }
        loadGroupMembers(group.id)
    }

    private fun loadGroupMembers(groupId: String) {
        groupMembersJob?.cancel()
        groupMembersJob = viewModelScope.launch {
            manageRoster.getStudentsInGroup(groupId)
                .catch { e -> _uiState.update { it.copy(errorMessage = e.message) } }
                .collect { athletes ->
                    _uiState.update { it.copy(athletesInGroup = athletes) }
                }
        }
    }

    private fun addGroup(name: String, location: String?, cycle: String?) {
        viewModelScope.launch {
            createGroup(name, location, cycle)
                .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            _uiState.update { it.copy(showAddGroupDialog = false) }
        }
    }

    private fun registerNewAthlete(action: RosterAction.OnRegisterAthlete) {
        viewModelScope.launch {
            registerAthlete(
                firstName = action.firstName,
                lastName = action.lastName,
                dateOfBirth = action.dateOfBirth,
                sex = action.sex,
                email = action.email,
                medicalAlert = action.medicalAlert
            ).onFailure { e -> _uiState.update { it.copy(errorMessage = e.message) } }
            _uiState.update { it.copy(showRegisterAthleteDialog = false) }
        }
    }

    private fun addAthleteToGroup(individualId: String) {
        val groupId = _uiState.value.selectedGroup?.id ?: return
        viewModelScope.launch {
            manageRoster.addStudentToGroup(groupId, individualId)
        }
    }

    private fun removeAthleteFromGroup(individualId: String) {
        val groupId = _uiState.value.selectedGroup?.id ?: return
        viewModelScope.launch {
            manageRoster.removeStudentFromGroup(groupId, individualId)
        }
    }
}
