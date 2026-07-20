package com.vamshi.field.domain.usecase.auth

import com.vamshi.field.domain.model.auth.User
import com.vamshi.field.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Resolves the account the Unlock screen should greet by default.
 *
 * Thin wrapper around [AuthRepository.getPrimaryAccount] — see that doc for the
 * single/multiple/zero-account resolution rule.
 */
class GetPrimaryAccountUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): User? = repository.getPrimaryAccount()
}
