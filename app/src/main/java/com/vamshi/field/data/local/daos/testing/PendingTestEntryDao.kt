package com.vamshi.field.data.local.daos.testing

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vamshi.field.data.local.entities.testing.PendingTestEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTestEntryDao {

    @Query("SELECT * FROM pending_test_entries WHERE eventId = :eventId ORDER BY stagedAt ASC")
    fun observeForEvent(eventId: String): Flow<List<PendingTestEntryEntity>>

    @Query("SELECT * FROM pending_test_entries WHERE eventId = :eventId ORDER BY stagedAt ASC")
    suspend fun getForEvent(eventId: String): List<PendingTestEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PendingTestEntryEntity)

    @Query("DELETE FROM pending_test_entries WHERE eventId = :eventId AND individualId = :individualId AND testId = :testId")
    suspend fun delete(eventId: String, individualId: String, testId: String)

    @Query("DELETE FROM pending_test_entries WHERE eventId = :eventId")
    suspend fun deleteAllForEvent(eventId: String)
}
