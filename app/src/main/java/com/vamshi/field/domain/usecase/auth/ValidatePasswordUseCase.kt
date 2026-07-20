package com.example.alearning.domain.usecase.auth

import javax.inject.Inject

/**
 * Validates a plain-text password before hashing and storage.
 *
 * Rules (configurable — document here if you change them):
 *  - Minimum 8 characters.
 *  - Must contain at least one letter (any case).
 *  - Must contain at least one digit.
 *
 * Rationale: a digit + letter requirement prevents purely numeric PINs and purely
 * alphabetic passphrases that are trivially guessable in an offline database context.
 */
class ValidatePasswordUseCase @Inject constructor() {

    companion object {
        const val MIN_LENGTH = 8
        private val HAS_LETTER = Regex(".*[a-zA-Z].*")
        private val HAS_DIGIT = Regex(".*[0-9].*")
    }

    operator fun invoke(password: String): ValidationResult {
        return when {
            password.length < MIN_LENGTH ->
                ValidationResult.Invalid("Password must be at least $MIN_LENGTH characters")
            !password.matches(HAS_LETTER) ->
                ValidationResult.Invalid("Password must contain at least one letter")
            !password.matches(HAS_DIGIT) ->
                ValidationResult.Invalid("Password must contain at least one digit")
            else -> ValidationResult.Valid
        }
    }
}
