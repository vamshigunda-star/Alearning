package com.example.alearning.ui.auth.signup

/** All user-initiated events from the sign-up screen. */
sealed interface SignUpAction {
    data class FirstNameChanged(val value: String) : SignUpAction
    data class LastNameChanged(val value: String) : SignUpAction
    data class UsernameChanged(val value: String) : SignUpAction
    data class PasswordChanged(val value: String) : SignUpAction
    data class SecurityQuestionChanged(val value: String) : SignUpAction
    data class SecurityAnswerChanged(val value: String) : SignUpAction
    data object TogglePasswordVisibility : SignUpAction
    data object Submit : SignUpAction
    data object DismissError : SignUpAction
    /** Called by the screen after consuming the [SignUpUiState.signUpSuccess] flag. */
    data object NavigationConsumed : SignUpAction
}
