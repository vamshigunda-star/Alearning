package com.example.alearning.data.local.daos.testing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.data.local.entities.testing.EventTestCrossRef
import com.example.alearning.data.local.entities.testing.TestResultEntity
import com.example.alearning.data.local.entities.testing.TestingEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TestingDao {

    // --- EVENTS ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: TestingEventEntity)

    @Query("SELECT * FROM testing_events ORDER BY date DESC")
    fun getAllEvents(): Flow<List<TestingEventEntity>>

    @Query("SELECT * FROM testing_events WHERE id = :eventId LIMIT 1")
    suspend fun getEventById(eventId: String): TestingEventEntity?

    // Feature: Show only events for a specific group (e.g., "Varsity History")
    @Query("SELECT * FROM testing_events WHERE groupId = :groupId ORDER BY date DESC")
    fun getEventsForGroup(groupId: String): Flow<List<TestingEventEntity>>

    // --- MENU BUILDING (Linking Tests to Events) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addTestToEvent(crossRef: EventTestCrossRef)

    /**
     * Feature: "What tests are we doing today?"
     * Joins the CrossRef with FitnessTest table to give you the actual Test Objects for the UI.
     */
    @Query("""
        SELECT fitness_tests.* FROM fitness_tests
        INNER JOIN event_test_cross_ref ON fitness_tests.id = event_test_cross_ref.testId
        WHERE event_test_cross_ref.eventId = :eventId
        ORDER BY event_test_cross_ref.sortOrder ASC
    """)
    fun getTestsForEvent(eventId: String): Flow<List<FitnessTestEntity>>

    // --- RESULTS (Scoring) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: TestResultEntity)

    // Feature: History Chart (Progress over time for one person on one test)
    @Query("""
        SELECT * FROM test_results 
        WHERE individualId = :individualId AND testId = :testId 
        ORDER BY createdAt ASC
    """)
    fun getHistoryForTest(individualId: String, testId: String): Flow<List<TestResultEntity>>

    // Feature: Leaderboard (All results for a specific event)
    @Query("SELECT * FROM test_results WHERE eventId = :eventId")
    fun getEventResults(eventId: String): Flow<List<TestResultEntity>>

    @Query("""
    SELECT * FROM test_results 
    WHERE individualId = :individualId 
    ORDER BY createdAt DESC
""")
    fun getAllResultsForIndividual(individualId: String): Flow<List<TestResultEntity>>


    @Query("""
    SELECT * FROM test_results
    WHERE individualId = :individualId
    AND createdAt = (
        SELECT MAX(createdAt) 
        FROM test_results AS inner_results
        WHERE inner_results.individualId = :individualId
        AND inner_results.testId = test_results.testId)""")
    suspend fun getLatestResultPerTestForIndividual(individualId: String): List<TestResultEntity>

    @Query("""
    SELECT test_results.* FROM test_results
    INNER JOIN group_members ON test_results.individualId = group_members.individualId
    WHERE group_members.groupId = :groupId 
    AND test_results.testId = :testId
    ORDER BY test_results.createdAt DESC
""")
    fun getGroupResultsForTest(groupId: String, testId: String): Flow<List<TestResultEntity>>



}