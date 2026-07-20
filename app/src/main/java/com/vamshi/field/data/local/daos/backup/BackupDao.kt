package com.vamshi.field.data.local.daos.backup

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.vamshi.field.data.local.entities.auth.UserEntity
import com.vamshi.field.data.local.entities.people.GroupEntity
import com.vamshi.field.data.local.entities.people.GroupMemberCrossRef
import com.vamshi.field.data.local.entities.people.IndividualEntity
import com.vamshi.field.data.local.entities.testing.EventTestCrossRef
import com.vamshi.field.data.local.entities.testing.TestResultEntity
import com.vamshi.field.data.local.entities.testing.TestingEventEntity

@Dao
interface BackupDao {

    @Query("SELECT * FROM individuals")
    suspend fun getAllIndividuals(): List<IndividualEntity>

    @Query("SELECT * FROM `groups`")
    suspend fun getAllGroups(): List<GroupEntity>

    @Query("SELECT * FROM group_members")
    suspend fun getAllGroupMembers(): List<GroupMemberCrossRef>

    @Query("SELECT * FROM testing_events")
    suspend fun getAllTestingEvents(): List<TestingEventEntity>

    @Query("SELECT * FROM event_test_cross_ref")
    suspend fun getAllEventTests(): List<EventTestCrossRef>

    @Query("SELECT * FROM test_results")
    suspend fun getAllTestResults(): List<TestResultEntity>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIndividuals(individuals: List<IndividualEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMembers(groupMembers: List<GroupMemberCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestingEvents(events: List<TestingEventEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventTests(eventTests: List<EventTestCrossRef>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTestResults(results: List<TestResultEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("DELETE FROM individuals")
    suspend fun clearIndividuals()

    @Query("DELETE FROM `groups`")
    suspend fun clearGroups()

    @Query("DELETE FROM group_members")
    suspend fun clearGroupMembers()

    @Query("DELETE FROM testing_events")
    suspend fun clearTestingEvents()

    @Query("DELETE FROM event_test_cross_ref")
    suspend fun clearEventTests()

    @Query("DELETE FROM test_results")
    suspend fun clearTestResults()

    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Transaction
    suspend fun clearAllUserGeneratedData() {
        clearGroupMembers()
        clearEventTests()
        clearTestResults()
        clearIndividuals()
        clearGroups()
        clearTestingEvents()
        clearUsers()
    }
}
