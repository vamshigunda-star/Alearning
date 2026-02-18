package com.example.alearning.domain.usecase.people

import com.example.alearning.data.local.entities.people.IndividualEntity
import com.example.alearning.domain.repository.PeopleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageRosterUseCase @Inject constructor(
    private val repository: PeopleRepository
) {
    suspend fun addStudentToGroup(groupId: String, studentId: String) {
        repository.addMemberToGroup(groupId, studentId)
    }

    suspend fun removeStudentFromGroup(groupId: String, studentId: String) {
        repository.removeMemberFromGroup(groupId, studentId)
    }

    fun getStudentsInGroup(groupId: String): Flow<List<IndividualEntity>> {
        return repository.getIndividualsInGroup(groupId)
    }
}
