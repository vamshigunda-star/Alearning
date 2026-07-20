package com.example.alearning.ui.auth.signin

/** All user-initiated events from the sign-in screen. */
sealed interface SignInAction {
    data class UsernameChanged(val value: String) : SignInAction
    data class PasswordChanged(val value: String) : SignInAction
    data object TogglePasswordVisibility : SignInAction
    data object Submit : SignInAction
    data object ResetPasswordClicked : SignInAction
    data object NavigateToSignUpClicked : SignInAction
    data object DismissError : SignInAction
    /** Called by the screen after consuming any navigation UiState flag. */
    data object NavigationConsumed : SignInAction
}
