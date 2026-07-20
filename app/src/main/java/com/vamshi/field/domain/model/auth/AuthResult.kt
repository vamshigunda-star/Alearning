package com.example.alearning.domain.model.auth

/**
 * Typed result wrapper for auth operations.
 *
 * Using a sealed class instead of Kotlin [Result] lets callers pattern-match
 * on specific [AuthError] variants without going through exception unwrapping.
 */
sealed class AuthResult {
    data class Success(val user: User) : AuthResult()
    data class Failure(val error: AuthError) : AuthResult()
}

/**
 * Enumeration of all possible auth failure modes.
 *
 * Security note: [InvalidCredentials] is intentionally generic — it is used for
 * both "username not found" and "wrong password" at the sign-in level to prevent
 * username enumeration attacks on the presentation layer.
 */
enum class AuthError {
    /** The requested username is already registered on this device. */
    UsernameTaken,

    /** No account with the given username exists. Used only in ResetPassword flow. */
    UsernameNotFound,

    /** Username/password combination does not match any stored account. */
    InvalidCredentials,

    /** Password does not satisfy the complexity rules. */
    WeakPassword,

    /** Username format is invalid (regex mismatch or length out of range). */
    InvalidUsername,

    /** First or last name is blank or exceeds the max length. */
    InvalidName,

    /** The provided security answer does not match the stored hash. */
    IncorrectSecurityAnswer,

    /** No security question was set for the given account (legacy or skipped). */
    NoSecurityQuestion,

    /** Catch-all for unexpected data-layer failures. */
    Unknown
}
