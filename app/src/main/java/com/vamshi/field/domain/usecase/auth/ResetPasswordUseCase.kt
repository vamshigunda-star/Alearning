package com.vamshi.field.domain.usecase.auth

import com.vamshi.field.domain.model.auth.AuthError
import com.vamshi.field.domain.model.auth.AuthResult
import com.vamshi.field.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Resets the password for an existing account using the security answer.
 *
 * Validation:
 *  1. New password must satisfy complexity rules.
 *  2. Confirm password must match new password (caller checks this before invoking).
 *  3. The repository layer verifies the security answer hash.
 */
class ResetPasswordUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val validatePassword: ValidatePasswordUseCase
) {
    suspend operator fun invoke(
        username: String,
        securityAnswer: String,
        newPassword: String
    ): AuthResult {
        val passwordResult = validatePassword(newPassword)
        if (passwordResult is ValidationResult.Invalid) {
            return AuthResult.Failure(AuthError.WeakPassword)
        }
        return repository.resetPassword(
            username = username,
            securityAnswer = securityAnswer,
            newPassword = newPassword
        )
    }
}
