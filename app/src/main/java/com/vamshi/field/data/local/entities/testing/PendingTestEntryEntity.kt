package com.vamshi.field.data.local.entities.testing

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index

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
