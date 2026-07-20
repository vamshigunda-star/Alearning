package com.vamshi.field.domain.usecase.auth

import com.vamshi.field.domain.model.auth.AuthError
import com.vamshi.field.domain.model.auth.AuthResult
import com.vamshi.field.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Validates credentials and starts a session for an existing coach account.
 *
 * Returns [AuthError.InvalidCredentials] for both "username not found" and
 * "wrong password" — never reveal which field was wrong.
 */
class SignInUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(username: String, password: String): AuthResult {
        if (username.isBlank() || password.isBlank()) {
            return AuthResult.Failure(AuthError.InvalidCredentials)
        }
        return repository.signIn(username = username, password = password)
    }
}
