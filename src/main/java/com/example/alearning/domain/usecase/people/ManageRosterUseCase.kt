package com.example.alearning.domain.usecase.people

import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.repository.PeopleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageRosterUseCase @Inject constructor(
    private val repository: PeopleRepository
) {
    suspend fun addStudentToGroup(groupId: String, individualId: String) {
        repository.addMemberToGroup(groupId, individualId)
    }

    suspend fun removeStudentFromGroup(groupId: String, individualId: String) {
        repository.removeMemberFromGroup(groupId, individualId)
    }

    fun getStudentsInGroup(groupId: String): Flow<List<Individual>> {
        return repository.getIndividualsInGroup(groupId)
    }
}
