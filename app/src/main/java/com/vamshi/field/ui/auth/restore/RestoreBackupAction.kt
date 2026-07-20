package com.vamshi.field.ui.auth.restore

/**
 * All user-initiated events from the restore-backup screen.
 *
 * The Google Sign-In launcher itself lives in the Screen composable (it needs an
 * Activity result contract, same pattern as [com.vamshi.field.ui.settings.SettingsScreen]) —
 * [GoogleSignInSucceeded]/[GoogleSignInFailed] are how its result is reported back in.
 */
sealed interface RestoreBackupAction {
    data object GoogleSignInSucceeded : RestoreBackupAction
    data class GoogleSignInFailed(val message: String) : RestoreBackupAction
    data object DismissError : RestoreBackupAction
    /** Called by the screen after consuming the [RestoreBackupUiState.restoreSuccess] flag. */
    data object NavigationConsumed : RestoreBackupAction
}
