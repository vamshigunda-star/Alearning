package com.example.alearning.data.local.entities.people

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "individualId"], // Composite Key: A person can't be in the same group twice
    foreignKeys = [
        ForeignKey(
            entity = GroupEntity::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.Companion.CASCADE // If Group is deleted, remove memberships
        ),
        ForeignKey(
            entity = IndividualEntity::class,
            parentColumns = ["id"],
            childColumns = ["individualId"],
            onDelete = ForeignKey.Companion.CASCADE // If Individual is deleted, remove memberships
        )
    ],
    indices = [
        Index(value = ["individualId"]), // Essential for "What groups is John in?"
        Index(value = ["groupId"])       // Essential for "Who is in the Cardio Group?"
    ]
)
data class GroupMemberCrossRef(
    val groupId: String,
    val individualId: String,
    val dateJoined: Long = System.currentTimeMillis() // Useful to track when they started
)