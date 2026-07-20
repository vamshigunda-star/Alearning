package com.example.alearning.domain.usecase.auth

import com.example.alearning.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Retrieves the security question for a given username.
 *
 * Returns `null` if no account exists for the username — the caller should
 * surface a generic "Username not found" message without revealing account existence.
 */
class GetSecurityQuestionUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(username: String): String? =
        repository.getSecurityQuestion(username)
}
