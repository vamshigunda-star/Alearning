package com.vamshi.field.domain.model.auth

/**
 * Domain model representing an authenticated coach account.
 *
 * This is intentionally free of password fields — sensitive credential data
 * never crosses the domain boundary. The data layer handles hashing and
 * verification internally.
 *
 * Terminology note: the class is named [User] internally (the authenticated
 * principal), but all user-facing strings use "Coach" per app terminology.
 */
data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    /** Normalized to lowercase+trimmed before storage. */
    val username: String,
    val createdAt: Long
)

/** Convenience computed property — returns "FirstName LastName". */
val User.fullName: String
    get() = "$firstName $lastName"
