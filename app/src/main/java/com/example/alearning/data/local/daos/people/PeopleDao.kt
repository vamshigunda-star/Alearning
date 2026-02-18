package com.example.alearning.data.local.daos.people

import androidx.room.*
import com.example.alearning.data.local.entities.people.GroupEntity
import com.example.alearning.data.local.entities.people.GroupMemberCrossRef
import com.example.alearning.data.local.entities.people.IndividualEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PeopleDao {

    // --- INDIVIDUALS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIndividual(individual: IndividualEntity)

    @Update
    suspend fun updateIndividual(individual: IndividualEntity)

    @Delete
    suspend fun deleteIndividual(individual: IndividualEntity)

    @Query("SELECT * FROM individuals WHERE id = :id LIMIT 1")
    suspend fun getIndividualById(id: String): IndividualEntity?

    @Query("SELECT * FROM individuals WHERE isDeleted = 0 ORDER BY firstName ASC")
    fun getAllIndividuals(): Flow<List<IndividualEntity>>

    // Feature: Search Bar (Finds students by name)
    @Query("""
        SELECT * FROM individuals 
        WHERE (firstName LIKE '%' || :query || '%' OR lastName LIKE '%' || :query || '%') 
        AND isDeleted = 0 
        ORDER BY firstName ASC
    """)
    fun searchIndividuals(query: String): Flow<List<IndividualEntity>>

    // --- GROUPS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity)

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    @Query("SELECT * FROM groups WHERE isDeleted = 0 ORDER BY name ASC")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM groups WHERE id = :id LIMIT 1")
    suspend fun getGroupById(id: String): GroupEntity?

    // --- ROSTERING (Many-to-Many Relationships) ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addMemberToGroup(crossRef: GroupMemberCrossRef)

    @Delete
    suspend fun removeMemberFromGroup(crossRef: GroupMemberCrossRef)

    // Feature: Get all students in a specific group (e.g., "Class 9A Roster")
    @Query("""
        SELECT individuals.* FROM individuals
        INNER JOIN group_members ON individuals.id = group_members.individualId
        WHERE group_members.groupId = :groupId AND individuals.isDeleted = 0
        ORDER BY individuals.lastName ASC
    """)
    fun getIndividualsInGroup(groupId: String): Flow<List<IndividualEntity>>

    // Feature: Get all groups a student belongs to (e.g., "John's Teams")
    @Query("""
        SELECT groups.* FROM groups
        INNER JOIN group_members ON groups.id = group_members.groupId
        WHERE group_members.individualId = :studentId AND groups.isDeleted = 0
    """)
    fun getGroupsForIndividual(studentId: String): Flow<List<GroupEntity>>
}