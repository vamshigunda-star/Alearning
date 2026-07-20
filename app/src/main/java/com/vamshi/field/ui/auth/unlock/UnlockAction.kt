package com.vamshi.field.ui.auth.unlock

/** All user-initiated events from the unlock screen. */
sealed interface UnlockAction {
    data class PasswordChanged(val value: String) : UnlockAction
    data object TogglePasswordVisibility : UnlockAction
    data object Submit : UnlockAction
    /** Only dispatched when [UnlockUiState.showAccountSwitcher] is true. */
    data class AccountSelected(val userId: String) : UnlockAction
    data object DismissError : UnlockAction
    /** Called by the screen after consuming the [UnlockUiState.unlockSuccess] flag. */
    data object NavigationConsumed : UnlockAction
}
