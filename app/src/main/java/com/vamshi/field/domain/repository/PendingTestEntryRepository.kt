package com.vamshi.field.domain.repository

import com.vamshi.field.domain.model.testing.PendingTestEntry
import kotlinx.coroutines.flow.Flow

interface PendingTestEntryRepository {

    fun observePendingForEvent(eventId: String): Flow<List<PendingTestEntry>>

    suspend fun getPendingForEvent(eventId: String): List<PendingTestEntry>

    suspend fun upsert(entry: PendingTestEntry)

    suspend fun delete(eventId: String, individualId: String, testId: String)

    suspend fun discardAllForEvent(eventId: String)
}
