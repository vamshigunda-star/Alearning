package com.vamshi.field.data.local.entities.testing

import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.vamshi.field.data.local.entities.people.GroupEntity
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
