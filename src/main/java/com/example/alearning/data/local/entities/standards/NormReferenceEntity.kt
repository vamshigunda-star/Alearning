package com.example.alearning.data.local.entities.standards

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.alearning.data.local.entities.people.BiologicalSex
import java.util.UUID

@Entity(
    tableName = "norm_references",
    foreignKeys = [
        ForeignKey(
            entity = FitnessTestEntity::class,
            parentColumns = ["id"],
            childColumns = ["testId"],
            onDelete = ForeignKey.CASCADE // If test is deleted, delete its norms
        )
    ],
    // FAST LOOKUP INDEX:
    // Enables instant querying for specific Age/Sex/Variant combinations
    indices = [
        Index(value = ["testId", "variant", "sex", "ageMin"])
    ]
)
data class NormReferenceEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val testId: String,

    // Allows different standard sets (e.g., "General", "Elite", "State 2024")
    // If empty/null, treat as "Default"
    val variant: String? = null,

    // Matching Criteria
    val sex: BiologicalSex,     // Match against Individual's sex
    val ageMin: Float,          // e.g., 14.0
    val ageMax: Float,          // e.g., 14.99 (covers the whole year)

    // The Performance Standard
    val minScore: Double,       // The raw score required (e.g., 8.5 shuttles)
    val maxScore: Double,       // Upper bound for this bracket

    // The Result
    val percentile: Int,        // e.g., 85 (The score the student gets)
    val classification: String? = null, // e.g., "Superior", "Needs Improvement"

    // Sync Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)