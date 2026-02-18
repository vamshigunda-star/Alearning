package com.example.alearning.data.local.entities.standards


import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.alearning.data.local.entities.standards.TestCategoryEntity
import java.util.UUID

@Entity(
    tableName = "fitness_tests",
    foreignKeys = [
        ForeignKey(
            entity = TestCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT // Don't delete a category if tests exist
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class FitnessTestEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val categoryId: String,

    val name: String,            // e.g., "20m Shuttle Run (Beep Test)"
    val unit: String,            // e.g., "level", "reps", "min:sec", "cm"

    // Critical for Analytics
    val isHigherBetter: Boolean, // True = Pushups, False = 1 Mile Run

    val description: String? = null, // Instructions for the teacher

    // Sync Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)