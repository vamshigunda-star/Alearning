package com.example.alearning.data.local.entities.people

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "groups",
    indices = [Index(value = ["cycle", "name"])] // Fast lookup: "Show me Winter 2025 Teams"
)
data class GroupEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // 1. Group Details
    val name: String,           // e.g., "Advanced Lifting", "Morning Cardio"
    val location: String?,      // e.g., "Weight Room B", "City Park", "Main Studio"

    // 2. Organization
    val cycle: String?,         // e.g., "Winter 2025", "Season 1", "Jan-Mar Block"
    val category: String?,      // e.g., "TEAM", "CLASS", "PERSONAL_TRAINING"

    // 3. Sync Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)