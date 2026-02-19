package com.example.alearning.data.repository

import com.example.alearning.data.local.daos.people.PeopleDao
import com.example.alearning.data.local.entities.people.GroupMemberCrossRef
import com.example.alearning.data.mapper.people.toDomain
import com.example.alearning.data.mapper.people.toEntity
import com.example.alearning.domain.model.people.Group
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.repository.PeopleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PeopleRepositoryImpl @Inject constructor(
    private val dao: PeopleDao
) : PeopleRepository {

    // --- INDIVIDUALS ---
    override fun getAllIndividuals(): Flow<List<Individual>> {
        return dao.getAllIndividuals().map { list -> list.map { it.toDomain() } }
    }

    override fun searchIndividuals(query: String): Flow<List<Individual>> {
        return dao.searchIndividuals(query).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getIndividualById(id: String): Individual? {
        return dao.getIndividualById(id)?.toDomain()
    }

    override suspend fun insertIndividual(individual: Individual) {
        dao.insertIndividual(individual.toEntity())
    }

    override suspend fun deleteIndividual(individual: Individual) {
        dao.deleteIndividual(individual.toEntity())
    }

    // --- GROUPS ---
    override fun getAllGroups(): Flow<List<Group>> {
        return dao.getAllGroups().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getGroupById(id: String): Group? {
        return dao.getGroupById(id)?.toDomain()
    }

    override suspend fun insertGroup(group: Group) {
        dao.insertGroup(group.toEntity())
    }

    override suspend fun deleteGroup(group: Group) {
        dao.deleteGroup(group.toEntity())
    }

    // --- ROSTERING ---
    override fun getIndividualsInGroup(groupId: String): Flow<List<Individual>> {
        return dao.getIndividualsInGroup(groupId).map { list -> list.map { it.toDomain() } }
    }

    override fun getGroupsForIndividual(studentId: String): Flow<List<Group>> {
        return dao.getGroupsForIndividual(studentId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun addMemberToGroup(groupId: String, individualId: String) {
        val crossRef = GroupMemberCrossRef(groupId = groupId, individualId = individualId)
        dao.addMemberToGroup(crossRef)
    }

    override suspend fun removeMemberFromGroup(groupId: String, individualId: String) {
        val crossRef = GroupMemberCrossRef(groupId = groupId, individualId = individualId)
        dao.removeMemberFromGroup(crossRef)
    }
}
