package com.example.alearning.domain.usecase.auth

import com.example.alearning.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Clears the active coaching session.
 *
 * Does not delete any data — the account remains on the device for future sign-in.
 */
class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke() {
        repository.signOut()
    }
}
