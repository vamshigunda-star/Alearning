package com.example.alearning.ui.auth.signin

/**
 * UI state for the sign-in screen.
 *
 * [isLoading] defaults to `false` — form is idle at launch.
 * Navigation is triggered by the screen reacting to [signInSuccess], [navigateToReset],
 * and [navigateToSignUp] via [androidx.compose.runtime.LaunchedEffect].
 */
data class SignInUiState(
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    /** Set to true when credentials are validated; screen navigates to Dashboard. */
    val signInSuccess: Boolean = false,

    /** Set to true when "Forgot password?" is tapped; screen navigates to ResetPassword. */
    val navigateToReset: Boolean = false,

    /** Set to true when "Create account" is tapped; screen navigates to SignUp. */
    val navigateToSignUp: Boolean = false
)
