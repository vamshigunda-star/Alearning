package com.example.alearning.data.repository

import com.example.alearning.data.local.daos.people.PeopleDao
import com.example.alearning.data.local.entities.people.GroupEntity
import com.example.alearning.data.local.entities.people.GroupMemberCrossRef
import com.example.alearning.data.local.entities.people.IndividualEntity
import com.example.alearning.domain.repository.PeopleRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class PeopleRepositoryImpl @Inject constructor(
    private val dao: PeopleDao
) : PeopleRepository {

    // --- INDIVIDUALS ---
    override fun getAllIndividuals(): Flow<List<IndividualEntity>> {
        return dao.getAllIndividuals()
    }

    override fun searchIndividuals(query: String): Flow<List<IndividualEntity>> {
        return dao.searchIndividuals(query)
    }

    override suspend fun getIndividualById(id: String): IndividualEntity? {
        return dao.getIndividualById(id)
    }

    override suspend fun insertIndividual(individual: IndividualEntity) {
        dao.insertIndividual(individual)
    }

    override suspend fun deleteIndividual(individual: IndividualEntity) {
        dao.deleteIndividual(individual)
    }

    // --- GROUPS ---
    override fun getAllGroups(): Flow<List<GroupEntity>> {
        return dao.getAllGroups()
    }

    override suspend fun getGroupById(id: String): GroupEntity? {
        return dao.getGroupById(id)
    }

    override suspend fun insertGroup(group: GroupEntity) {
        dao.insertGroup(group)
    }

    override suspend fun deleteGroup(group: GroupEntity) {
        dao.deleteGroup(group)
    }

    // --- ROSTERING ---
    override fun getIndividualsInGroup(groupId: String): Flow<List<IndividualEntity>> {
        return dao.getIndividualsInGroup(groupId)
    }

    override fun getGroupsForIndividual(studentId: String): Flow<List<GroupEntity>> {
        return dao.getGroupsForIndividual(studentId)
    }

    override suspend fun addMemberToGroup(groupId: String, individualId: String) {
        // We create the CrossRef entity here, keeping the UI clean
        val crossRef = GroupMemberCrossRef(groupId = groupId, individualId = individualId)
        dao.addMemberToGroup(crossRef)
    }

    override suspend fun removeMemberFromGroup(groupId: String, individualId: String) {
        val crossRef = GroupMemberCrossRef(groupId = groupId, individualId = individualId)
        dao.removeMemberFromGroup(crossRef)
    }
}