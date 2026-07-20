package com.example.alearning.data.local.entities.testing

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.alearning.data.local.entities.people.GroupEntity
import java.util.UUID

@Entity(
    tableName = "testing_events",
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("groupId")]
)
data class TestingEventEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // Made Optional (Nullable)
    // If null, this is a personal/individual testing session
    val groupId: String? = null,

    val name: String,           // e.g., "My Morning Workout"
    val date: Long,
    val location: String? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)