package com.vamshi.field.domain.usecase.auth

import javax.inject.Inject

/**
 * Validates a first or last name field.
 *
 * Rules (documented here as the single source of truth):
 *  - After trimming, must be non-blank.
 *  - Maximum 50 characters.
 */
class ValidateNameUseCase @Inject constructor() {

    operator fun invoke(name: String): ValidationResult {
        val trimmed = name.trim()
        return when {
            trimmed.isBlank() -> ValidationResult.Invalid("Name cannot be blank")
            trimmed.length > 50 -> ValidationResult.Invalid("Name must be 50 characters or fewer")
            else -> ValidationResult.Valid
        }
    }
}
