package com.vamshi.field.domain.usecase.auth

import com.vamshi.field.domain.model.auth.User
import com.vamshi.field.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Reactively observes the currently signed-in [User].
 *
 * Emits `null` when no session is active or after sign-out.
 * Used by both the auth gate (start-destination decision) and
 * DashboardViewModel (coach name greeting).
 */
class ObserveCurrentUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): Flow<User?> = repository.observeCurrentUser()
}
