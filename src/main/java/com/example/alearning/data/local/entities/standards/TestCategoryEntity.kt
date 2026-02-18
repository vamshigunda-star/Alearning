package com.example.alearning.data.local.entities.standards

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "test_categories")
data class TestCategoryEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    val name: String,          // e.g., "Cardiorespiratory Endurance"
    val sortOrder: Int = 0,    // To control display order in the UI

    // Sync Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)