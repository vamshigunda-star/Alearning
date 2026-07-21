package com.vamshi.field.ui.auth.restore

import com.vamshi.field.domain.model.backup.DriveBackupSummary

/**
 * UI state for the pre-auth "restore from Google Drive" screen.
 *
 * Reachable from both Onboarding ("Restore existing data") and Unlock ("Trouble signing
 * in?") — for a coach reinstalling the app who already has a Drive backup, this route
 * skips onboarding/unlock entirely: the restored payload re-establishes the whole
 * account and session, so there's no password field or security question here.
 *
 * Sign-in is followed by a device picker: [availableBackups] lists every backup found for
 * the signed-in account (one per device that has ever backed up) so the coach can pick the
 * right one instead of always getting an arbitrary "the" backup. [isLoadingBackups] covers
 * fetching that list; [isRestoring] covers the actual download + DB-transaction restore
 * once a backup is picked — both are real network operations, not instant.
 */
data class RestoreBackupUiState(
    val isLoadingBackups: Boolean = false,
    val availableBackups: List<DriveBackupSummary> = emptyList(),
    val isRestoring: Boolean = false,
    val errorMessage: String? = null,
    val restoreSuccess: Boolean = false
)
