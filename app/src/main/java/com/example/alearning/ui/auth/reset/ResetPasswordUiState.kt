package com.example.alearning.ui.auth.reset

/**
 * UI state for the two-step password reset screen.
 *
 * Step 1: Coach enters username, taps Continue.
 *         [securityQuestion] is populated on success.
 * Step 2: Coach enters security answer + new password (with confirm), taps Submit.
 *         On success: [resetSuccess] becomes true — screen shows a Snackbar and
 *         navigates back to SignIn via LaunchedEffect.
 */
data class ResetPasswordUiState(
    // Step control
    val step: Int = 1,

    // Step 1 fields
    val username: String = "",

    // Populated after Step 1 success
    val securityQuestion: String? = null,

    // Step 2 fields
    val securityAnswer: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val newPasswordVisible: Boolean = false,

    // Shared
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val resetSuccess: Boolean = false,

    // Per-field errors (Step 2)
    val securityAnswerError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null
)
