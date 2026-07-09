package com.example.alearning.ui.auth.signup

/**
 * UI state for the sign-up screen.
 *
 * [isLoading] defaults to `false` — the form starts idle and becomes `true` only
 * during the async submit operation.
 *
 * Per-field error strings are nullable: null means "no error shown"; a non-null
 * string is displayed as the OutlinedTextField supporting text with isError=true.
 *
 * [signUpSuccess] is the UiState flag used by the screen's LaunchedEffect to
 * trigger navigation to Dashboard without passing nav callbacks into the ViewModel.
 */
data class SignUpUiState(
    val firstName: String = "",
    val lastName: String = "",
    val username: String = "",
    val password: String = "",
    val securityQuestion: String = "",
    val securityAnswer: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val signUpSuccess: Boolean = false,

    // Per-field validation error strings
    val firstNameError: String? = null,
    val lastNameError: String? = null,
    val usernameError: String? = null,
    val passwordError: String? = null,
    val securityQuestionError: String? = null,
    val securityAnswerError: String? = null
)
