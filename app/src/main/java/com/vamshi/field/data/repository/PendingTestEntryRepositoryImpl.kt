package com.example.alearning.data.repository

import com.example.alearning.data.local.daos.testing.PendingTestEntryDao
import com.example.alearning.data.mapper.testing.toDomain
import com.example.alearning.data.mapper.testing.toEntity
import com.example.alearning.domain.model.testing.PendingTestEntry
import com.example.alearning.domain.repository.PendingTestEntryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PendingTestEntryRepositoryImpl @Inject constructor(
    private val dao: PendingTestEntryDao
) : PendingTestEntryRepository {

    override fun observePendingForEvent(eventId: String): Flow<List<PendingTestEntry>> =
        dao.observeForEvent(eventId).map { list -> list.map { it.toDomain() } }

    override suspend fun getPendingForEvent(eventId: String): List<PendingTestEntry> =
        dao.getForEvent(eventId).map { it.toDomain() }

    override suspend fun upsert(entry: PendingTestEntry) {
        dao.upsert(entry.toEntity())
    }

    override suspend fun delete(eventId: String, individualId: String, testId: String) {
        dao.delete(eventId, individualId, testId)
    }

    override suspend fun discardAllForEvent(eventId: String) {
        dao.deleteAllForEvent(eventId)
    }
}
