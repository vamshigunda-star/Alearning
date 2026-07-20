package com.vamshi.field.domain.usecase.people

import com.vamshi.field.domain.model.people.Individual
import com.vamshi.field.domain.repository.PeopleRepository
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
