package com.example.alearning.domain.repository

import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.testing.TestResult
import com.example.alearning.domain.model.testing.TestingEvent
import kotlinx.coroutines.flow.Flow

interface TestingRepository {

    // --- Events ---
    fun getAllEvents(): Flow<List<TestingEvent>>

    fun getEventsForGroup(groupId: String): Flow<List<TestingEvent>>

    suspend fun getEventById(eventId: String): TestingEvent?

    /**
     * Creates an event and automatically sets up the "Menu" of tests for it.
     * @param event The event details (name, date, etc)
     * @param testIds A list of FitnessTest IDs that will be performed at this event.
     */
    suspend fun createEvent(event: TestingEvent, testIds: List<String>)

    // --- Menu Retrieval ---
    fun getTestsForEvent(eventId: String): Flow<List<FitnessTest>>

    // --- Results ---
    suspend fun saveResult(result: TestResult)

    fun getHistoryForTest(individualId: String, testId: String): Flow<List<TestResult>>

    fun getEventResults(eventId: String): Flow<List<TestResult>>

    fun getAllResultsForIndividual(individualId: String): Flow<List<TestResult>>
}
