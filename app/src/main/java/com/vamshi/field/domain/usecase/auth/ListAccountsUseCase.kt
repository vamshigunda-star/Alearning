package com.vamshi.field.domain.usecase.auth

import com.vamshi.field.domain.model.auth.User
import com.vamshi.field.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Lists every coach account stored on this device, most recently created first.
 *
 * Used only to power the Unlock screen's rare multi-account switcher, which the
 * screen hides entirely when there's a single account.
 */
class ListAccountsUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): List<User> = repository.listAccounts()
}
