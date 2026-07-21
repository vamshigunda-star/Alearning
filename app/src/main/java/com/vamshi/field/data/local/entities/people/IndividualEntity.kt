package com.vamshi.field.data.local.entities.people

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Index
import java.util.UUID
import com.vamshi.field.domain.model.people.BiologicalSex


@Entity(
    tableName = "individuals",
    indices = [
        Index(value = ["lastName", "firstName"]), // Fast sorting by name
        Index(value = ["isActive"])               // Fast filtering of active clients
    ]
)
data class IndividualEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    // 1. Identity
    val firstName: String,
    val lastName: String,

    // 2. Physiology (Crucial for Norms/Stats)
    val dateOfBirth: Long,      // Stored as Epoch Millis
    val sex: BiologicalSex,     // Needed for gender-based performance standards

    // 3. Safety / Medical (Crucial for Performance Apps)
    val medicalAlert: String? = null, // e.g. "Previous ACL tear", "Asthma"
    val isRestricted: Boolean = false, // UI Flag: Red warning icon if true

    // 4. Admin
    val email: String? = null,  // Optional: for reporting or identification
    val isActive: Boolean = true, // If they cancel membership/leave, set false (don't delete)
    val notes: String? = null,

    // 5. Sync Metadata
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
