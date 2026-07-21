package com.vamshi.field.data.local.entities.testing

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import com.vamshi.field.data.local.entities.standards.FitnessTestEntity

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
