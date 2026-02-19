package com.example.alearning.domain.model.people


// This enum should live in the Domain layer.
// The Data layer (Entity) will import it from here.
enum class BiologicalSex { MALE, FEMALE, UNSPECIFIED }

/**
 * Clean Domain Model for an Individual (Student/Athlete).
 * Notice: No @Entity, no @PrimaryKey, no Room imports!
 */
data class Individual(
    val id: String,

    // 1. Identity
    val firstName: String,
    val lastName: String,

    // 2. Physiology
    val dateOfBirth: Long, // Changed from Long to a proper Date object
    val sex: BiologicalSex,

    // 3. Safety / Medical
    val medicalAlert: String? = null,
    val isRestricted: Boolean = false,

    // 4. Admin
    val email: String? = null,
    val isActive: Boolean = true,
    val notes: String? = null

    // Note: We intentionally DROPPED isDeleted, createdAt, and updatedAt.
    // The UI and Business Logic don't need to know about database sync metadata.
) {
    // --- BUSINESS LOGIC (Helpers for UI and Use Cases) ---

    /**
     * Easily grab the full name for UI display.
     */
    val fullName: String
        get() = "$firstName $lastName"

    /**
     * CRITICAL for Fitness Testing: Automatically calculates the student's
     * current age in years. You will pass this into your StandardsDao norm lookup!
     */
    val currentAge: Int
        get() = ((System.currentTimeMillis() - dateOfBirth) / 31_557_600_000L).toInt()
}