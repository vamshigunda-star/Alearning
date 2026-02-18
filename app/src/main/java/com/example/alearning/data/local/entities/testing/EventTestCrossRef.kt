package com.example.alearning.data.local.entities.testing

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import com.example.alearning.data.local.entities.standards.FitnessTestEntity

@Entity(
    tableName = "event_test_cross_ref",
    primaryKeys = ["eventId", "testId"],
    foreignKeys = [
        ForeignKey(
            entity = TestingEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FitnessTestEntity::class,
            parentColumns = ["id"],
            childColumns = ["testId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("testId")]
)
data class EventTestCrossRef(
    val eventId: String,
    val testId: String,
    val sortOrder: Int = 0
)