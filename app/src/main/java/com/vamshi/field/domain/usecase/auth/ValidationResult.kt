package com.example.alearning.domain.usecase.auth

/**
 * Result of a field-level validation check.
 *
 * Used by [ValidateNameUseCase], [ValidateUsernameUseCase], and
 * [ValidatePasswordUseCase] to surface inline field errors in UiState
 * without coupling the presentation layer to string resources at the domain level.
 */
sealed class ValidationResult {
    /** The field value satisfies all rules. */
    data object Valid : ValidationResult()

    /**
     * The field value is invalid.
     *
     * @param reason Human-readable explanation, suitable for display as a
     *               supporting text beneath an [androidx.compose.material3.OutlinedTextField].
     */
    data class Invalid(val reason: String) : ValidationResult()
}
