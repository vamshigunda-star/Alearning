package com.example.alearning.domain.repository

import com.example.alearning.domain.model.people.Group
import com.example.alearning.domain.model.people.Individual

import kotlinx.coroutines.flow.Flow

interface PeopleRepository {

    // --- Individuals ---
    fun getAllIndividuals(): Flow<List<Individual>>

    fun searchIndividuals(query: String): Flow<List<Individual>>

    suspend fun getIndividualById(id: String): Individual?

    suspend fun insertIndividual(individual: Individual)

    suspend fun deleteIndividual(individual: Individual)

    // --- Groups ---
    fun getAllGroups(): Flow<List<Group>>

    suspend fun getGroupById(id: String): Group?

    suspend fun insertGroup(group: Group)

    suspend fun deleteGroup(group: Group)

    // --- Rostering (The "Junction" Logic) ---
    // Note: In the domain layer, we prefer to talk about "adding a student to a Group"
    // rather than "inserting a cross-ref entity."

    fun getIndividualsInGroup(groupId: String): Flow<List<Individual>>

    fun getGroupsForIndividual(individualId: String): Flow<List<Group>>

    suspend fun addMemberToGroup(groupId: String, individualId: String)

    suspend fun removeMemberFromGroup(groupId: String, individualId: String)
}