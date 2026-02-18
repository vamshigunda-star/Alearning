package com.example.alearning.data.local.entities.testing

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.alearning.data.local.entities.people.IndividualEntity
import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import java.util.UUID

@Entity(
    tableName = "test_results",
    foreignKeys = [
        ForeignKey(
            entity = TestingEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.Companion.CASCADE
        ),
        ForeignKey(
            entity = IndividualEntity::class,
            parentColumns = ["id"],
            childColumns = ["individualId"],
            onDelete = ForeignKey.Companion.CASCADE
        ),
        ForeignKey(
            entity = FitnessTestEntity::class,
            parentColumns = ["id"],
            childColumns = ["testId"],
            onDelete = ForeignKey.Companion.RESTRICT
        )
    ],
    indices = [Index("eventId"), Index("individualId"), Index("testId")]
)
data class TestResultEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val eventId: String,
    val individualId: String,
    val testId: String,

    val rawScore: Double,

    // We store these as a "snapshot" so history remains accurate
    // even if the Individual's profile or age changes later.
    val ageAtTime: Float,
    val weightAtTime: Double? = null,
    val bodyWeightKg: Double? = null, // Optional: useful for power-to-weight tests

    // 3. The Interpretation
    val percentile: Int? = null,           // e.g., 92
    val classification: String? = null,    // e.g., "High Performance"
    val normVariantUsed: String? = null,   // e.g., "Standard 2025"

    val createdAt: Long = System.currentTimeMillis()
)