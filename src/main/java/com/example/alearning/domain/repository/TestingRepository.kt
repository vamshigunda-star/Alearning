package com.example.alearning.domain.repository

import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.data.local.entities.testing.TestResultEntity
import com.example.alearning.data.local.entities.testing.TestingEventEntity
import kotlinx.coroutines.flow.Flow

interface TestingRepository {

    // --- Events ---
    fun getAllEvents(): Flow<List<TestingEventEntity>>

    fun getEventsForGroup(groupId: String): Flow<List<TestingEventEntity>>

    suspend fun getEventById(eventId: String): TestingEventEntity?

    /**
     * Creates an event and automatically sets up the "Menu" of tests for it.
     * @param event The event details (name, date, etc)
     * @param testIds A list of FitnessTest IDs that will be performed at this event.
     */
    suspend fun createEvent(event: TestingEventEntity, testIds: List<String>)

    // --- Menu Retrieval ---
    fun getTestsForEvent(eventId: String): Flow<List<FitnessTestEntity>>

    // --- Results ---
    suspend fun saveResult(result: TestResultEntity)

    fun getHistoryForTest(individualId: String, testId: String): Flow<List<TestResultEntity>>

    fun getEventResults(eventId: String): Flow<List<TestResultEntity>>
}