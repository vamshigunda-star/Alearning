package com.example.alearning.data.repository

import com.example.alearning.data.local.daos.testing.TestingDao
import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.data.local.entities.testing.EventTestCrossRef
import com.example.alearning.data.local.entities.testing.TestResultEntity
import com.example.alearning.data.local.entities.testing.TestingEventEntity
import com.example.alearning.domain.repository.TestingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TestingRepositoryImpl @Inject constructor(
    private val dao: TestingDao
) : TestingRepository {

    // --- EVENTS ---
    override fun getAllEvents(): Flow<List<TestingEventEntity>> {
        return dao.getAllEvents()
    }

    override fun getEventsForGroup(groupId: String): Flow<List<TestingEventEntity>> {
        return dao.getEventsForGroup(groupId)
    }

    override suspend fun getEventById(eventId: String): TestingEventEntity? {
        return dao.getEventById(eventId)
    }

    override suspend fun createEvent(event: TestingEventEntity, testIds: List<String>) {
        // 1. Save the event details
        dao.insertEvent(event)

        // 2. Loop through the test IDs and link them to this event
        // We use the index (0, 1, 2) as the sort order
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
    override fun getTestsForEvent(eventId: String): Flow<List<FitnessTestEntity>> {
        return dao.getTestsForEvent(eventId)
    }

    // --- RESULTS ---
    override suspend fun saveResult(result: TestResultEntity) {
        dao.insertResult(result)
    }

    override fun getHistoryForTest(individualId: String, testId: String): Flow<List<TestResultEntity>> {
        return dao.getHistoryForTest(individualId, testId)
    }

    override fun getEventResults(eventId: String): Flow<List<TestResultEntity>> {
        return dao.getEventResults(eventId)
    }
}