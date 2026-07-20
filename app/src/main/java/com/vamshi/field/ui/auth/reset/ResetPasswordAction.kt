package com.vamshi.field.ui.auth.reset

/** All user-initiated events from the password reset screen. */
sealed interface ResetPasswordAction {
    data class UsernameChanged(val value: String) : ResetPasswordAction
    data object ContinueClicked : ResetPasswordAction
    data class AnswerChanged(val value: String) : ResetPasswordAction
    data class NewPasswordChanged(val value: String) : ResetPasswordAction
    data class ConfirmPasswordChanged(val value: String) : ResetPasswordAction
    data object ToggleNewPasswordVisibility : ResetPasswordAction
    data object Submit : ResetPasswordAction
    data object BackClicked : ResetPasswordAction
    data object DismissError : ResetPasswordAction
    /** Called by the screen after consuming the [ResetPasswordUiState.resetSuccess] flag. */
    data object NavigationConsumed : ResetPasswordAction
}
