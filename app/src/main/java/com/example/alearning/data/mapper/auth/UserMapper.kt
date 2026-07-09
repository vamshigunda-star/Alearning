package com.example.alearning.data.mapper.auth

import com.example.alearning.data.local.entities.auth.UserEntity
import com.example.alearning.domain.model.auth.User

/**
 * Mapping extensions between [UserEntity] (data layer) and [User] (domain layer).
 *
 * Security contract: [toDomain] intentionally drops all password and security-answer
 * fields — they must never cross into the domain or presentation layers.
 */

/** Maps [UserEntity] to the domain [User], stripping all credential fields. */
fun UserEntity.toDomain(): User = User(
    id = id,
    firstName = firstName,
    lastName = lastName,
    username = username,
    createdAt = createdAt
)
