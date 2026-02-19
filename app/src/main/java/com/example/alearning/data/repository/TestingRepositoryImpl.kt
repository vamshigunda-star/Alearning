package com.example.alearning.data.repository

import com.example.alearning.data.local.daos.testing.TestingDao
import com.example.alearning.data.local.entities.testing.EventTestCrossRef
import com.example.alearning.data.mapper.standards.toDomain
import com.example.alearning.data.mapper.testing.toDomain
import com.example.alearning.data.mapper.testing.toEntity
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.testing.TestResult
import com.example.alearning.domain.model.testing.TestingEvent
import com.example.alearning.domain.repository.TestingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TestingRepositoryImpl @Inject constructor(
    private val dao: TestingDao
) : TestingRepository {

    // --- EVENTS ---
    override fun getAllEvents(): Flow<List<TestingEvent>> {
        return dao.getAllEvents().map { list -> list.map { it.toDomain() } }
    }

    override fun getEventsForGroup(groupId: String): Flow<List<TestingEvent>> {
        return dao.getEventsForGroup(groupId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getEventById(eventId: String): TestingEvent? {
        return dao.getEventById(eventId)?.toDomain()
    }

    override suspend fun createEvent(event: TestingEvent, testIds: List<String>) {
        // 1. Save the event details
        dao.insertEvent(event.toEntity())

        // 2. Loop through the test IDs and link them to this event
        testIds.forEachIndexed { index, testId ->
            val crossRef = EventTestCrossRef(
                eventId = event.id,
                testId = testId,
                sortOrder = index
            )
            dao.addTestToEvent(crossRef)
        }
    }

    // --- MENU ---
    override fun getTestsForEvent(eventId: String): Flow<List<FitnessTest>> {
        return dao.getTestsForEvent(eventId).map { list -> list.map { it.toDomain() } }
    }

    // --- RESULTS ---
    override suspend fun saveResult(result: TestResult) {
        dao.insertResult(result.toEntity())
    }

    override fun getHistoryForTest(individualId: String, testId: String): Flow<List<TestResult>> {
        return dao.getHistoryForTest(individualId, testId).map { list -> list.map { it.toDomain() } }
    }

    override fun getEventResults(eventId: String): Flow<List<TestResult>> {
        return dao.getEventResults(eventId).map { list -> list.map { it.toDomain() } }
    }
}
