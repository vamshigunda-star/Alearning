package com.example.alearning.ui.roster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.model.people.Group
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.usecase.people.CreateGroupUseCase
import com.example.alearning.domain.usecase.people.ManageRosterUseCase
import com.example.alearning.domain.usecase.people.RegisterAthleteUseCase
import com.example.alearning.domain.repository.PeopleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RosterUiState(
    val groups: List<Group> = emptyList(),
    val selectedGroup: Group? = null,
    val studentsInGroup: List<Individual> = emptyList(),
    val allStudents: List<Individual> = emptyList(),
    val showAddGroupDialog: Boolean = false,
    val showAddStudentDialog: Boolean = false,
    val showAddToGroupDialog: Boolean = false
)

@HiltViewModel
class RosterViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val registerStudent: RegisterAthleteUseCase,
    private val createGroup: CreateGroupUseCase,
    private val manageRoster: ManageRosterUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RosterUiState())
    val uiState: StateFlow<RosterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            peopleRepository.getAllGroups().collect { groups ->
                _uiState.value = _uiState.value.copy(groups = groups)
                if (_uiState.value.selectedGroup == null && groups.isNotEmpty()) {
                    selectGroup(groups.first())
                }
            }
        }
        viewModelScope.launch {
            peopleRepository.getAllIndividuals().collect { students ->
                _uiState.value = _uiState.value.copy(allStudents = students)
            }
        }
    }

    fun selectGroup(group: Group) {
        _uiState.value = _uiState.value.copy(selectedGroup = group)
        viewModelScope.launch {
            manageRoster.getStudentsInGroup(group.id).collect { students ->
                _uiState.value = _uiState.value.copy(studentsInGroup = students)
            }
        }
    }

    fun showAddGroupDialog() {
        _uiState.value = _uiState.value.copy(showAddGroupDialog = true)
    }

    fun dismissAddGroupDialog() {
        _uiState.value = _uiState.value.copy(showAddGroupDialog = false)
    }

    fun showAddStudentDialog() {
        _uiState.value = _uiState.value.copy(showAddStudentDialog = true)
    }

    fun dismissAddStudentDialog() {
        _uiState.value = _uiState.value.copy(showAddStudentDialog = false)
    }

    fun showAddToGroupDialog() {
        _uiState.value = _uiState.value.copy(showAddToGroupDialog = true)
    }

    fun dismissAddToGroupDialog() {
        _uiState.value = _uiState.value.copy(showAddToGroupDialog = false)
    }

    fun addGroup(name: String, location: String?, cycle: String?) {
        viewModelScope.launch {
            createGroup(name, location, cycle)
            _uiState.value = _uiState.value.copy(showAddGroupDialog = false)
        }
    }

    fun addStudent(firstName: String, lastName: String, dateOfBirth: Long, sex: BiologicalSex) {
        viewModelScope.launch {
            registerStudent(firstName, lastName, dateOfBirth, sex)
            _uiState.value = _uiState.value.copy(showAddStudentDialog = false)
        }
    }

    fun addStudentToGroup(studentId: String) {
        val groupId = _uiState.value.selectedGroup?.id ?: return
        viewModelScope.launch {
            manageRoster.addStudentToGroup(groupId, studentId)
        }
    }

    fun removeStudentFromGroup(studentId: String) {
        val groupId = _uiState.value.selectedGroup?.id ?: return
        viewModelScope.launch {
            manageRoster.removeStudentFromGroup(groupId, studentId)
        }
    }
}
