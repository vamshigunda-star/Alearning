package com.vamshi.field.ui.auth.unlock

import com.vamshi.field.domain.model.auth.User

/**
 * UI state for the "Welcome back" unlock screen — the returning-coach counterpart to
 * [com.vamshi.field.ui.auth.onboarding.OnboardingScreen].
 *
 * [isLoading] starts `true`: the screen needs to resolve the account to greet
 * ([accountId]/[coachDisplayName]) before it can render, then flips to `false` once
 * that lookup (and the account list, for [showAccountSwitcher]) resolves. It is set
 * back to `true` briefly during the password-submit round trip.
 *
 * [showAccountSwitcher] is only ever `true` when more than one account exists on the
 * device (a rare case — typically only after a Drive restore merges accounts). The
 * screen hides the switcher row entirely otherwise.
 */
data class UnlockUiState(
    val accountId: String? = null,
    val coachDisplayName: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val showAccountSwitcher: Boolean = false,
    val accounts: List<User> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val unlockSuccess: Boolean = false
)
