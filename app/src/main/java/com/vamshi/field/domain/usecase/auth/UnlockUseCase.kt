package com.vamshi.field.domain.usecase.auth

import com.vamshi.field.domain.model.auth.AuthResult
import com.vamshi.field.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Validates the password for a known account (resolved by the Unlock screen via
 * [GetPrimaryAccountUseCase] or [ListAccountsUseCase]) and establishes a session.
 *
 * Thin wrapper around [AuthRepository.unlock] — the Unlock-screen equivalent of
 * the old [SignInUseCase], but keyed by account ID instead of a typed username.
 */
class UnlockUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(userId: String, password: String): AuthResult =
        repository.unlock(userId, password)
}
