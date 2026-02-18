package com.example.alearning.domain.repository

import com.example.alearning.data.local.entities.people.GroupEntity
import com.example.alearning.data.local.entities.people.IndividualEntity
import kotlinx.coroutines.flow.Flow

interface PeopleRepository {

    // --- Individuals ---
    fun getAllIndividuals(): Flow<List<IndividualEntity>>

    fun searchIndividuals(query: String): Flow<List<IndividualEntity>>

    suspend fun getIndividualById(id: String): IndividualEntity?

    suspend fun insertIndividual(individual: IndividualEntity)

    suspend fun deleteIndividual(individual: IndividualEntity)

    // --- Groups ---
    fun getAllGroups(): Flow<List<GroupEntity>>

    suspend fun getGroupById(id: String): GroupEntity?

    suspend fun insertGroup(group: GroupEntity)

    suspend fun deleteGroup(group: GroupEntity)

    // --- Rostering (The "Junction" Logic) ---
    // Note: In the domain layer, we prefer to talk about "adding a student to a group"
    // rather than "inserting a cross-ref entity."

    fun getIndividualsInGroup(groupId: String): Flow<List<IndividualEntity>>

    fun getGroupsForIndividual(studentId: String): Flow<List<GroupEntity>>

    suspend fun addMemberToGroup(groupId: String, individualId: String)

    suspend fun removeMemberFromGroup(groupId: String, individualId: String)
}