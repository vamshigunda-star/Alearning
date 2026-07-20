package com.vamshi.field.domain.usecase.auth

import javax.inject.Inject

/**
 * Validates a username string before account creation or sign-in.
 *
 * Rules (the authoritative definition):
 *  - After lowercasing and trimming, must match [USERNAME_REGEX].
 *  - Allowed characters: lowercase letters, digits, dots (.), underscores (_), hyphens are NOT allowed.
 *  - Length: 3–30 characters.
 *
 * The domain layer keeps this regex as a pure Kotlin constant — no Android imports.
 */
class ValidateUsernameUseCase @Inject constructor() {

    companion object {
        /** Authoritative username format. Must match exactly after normalization. */
        val USERNAME_REGEX = Regex("^[a-z0-9._]{3,30}$")
    }

    operator fun invoke(username: String): ValidationResult {
        val normalized = username.trim().lowercase()
        return when {
            normalized.isBlank() -> ValidationResult.Invalid("Username cannot be blank")
            !normalized.matches(USERNAME_REGEX) -> ValidationResult.Invalid(
                "Username must be 3–30 characters: letters, digits, . or _ only"
            )
            else -> ValidationResult.Valid
        }
    }
}
