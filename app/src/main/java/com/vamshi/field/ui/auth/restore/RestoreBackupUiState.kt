package com.vamshi.field.ui.auth.restore

/**
 * UI state for the pre-auth "restore from Google Drive" screen.
 *
 * Reachable from both Onboarding ("Restore existing data") and Unlock ("Trouble signing
 * in?") — for a coach reinstalling the app who already has a Drive backup, this route
 * skips onboarding/unlock entirely: the restored payload re-establishes the whole
 * account and session, so there's no password field or security question here.
 *
 * [isLoading] covers the real network + DB-transaction restore operation, which is not
 * instant — the screen shows a persistent loading state during it, not just a button spinner.
 */
data class RestoreBackupUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val restoreSuccess: Boolean = false
)
