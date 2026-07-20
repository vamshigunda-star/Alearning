package com.vamshi.field.data.mapper.auth

import com.vamshi.field.data.local.entities.auth.UserEntity
import com.vamshi.field.domain.model.auth.User

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
    email = email,
    createdAt = createdAt
)
