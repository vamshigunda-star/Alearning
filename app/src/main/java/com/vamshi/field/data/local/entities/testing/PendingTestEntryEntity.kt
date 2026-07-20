package com.example.alearning.data.local.entities.testing

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "pending_test_entries",
    primaryKeys = ["eventId", "individualId", "testId"],
    foreignKeys = [
        ForeignKey(
            entity = TestingEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eventId")]
)
data class PendingTestEntryEntity(
    val eventId: String,
    val individualId: String,
    val testId: String,
    val rawScore: Double,
    val stagedAt: Long
)
