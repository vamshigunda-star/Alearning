package com.example.alearning.domain.repository

import com.example.alearning.domain.model.auth.AuthResult
import com.example.alearning.domain.model.auth.User
import kotlinx.coroutines.flow.Flow

/**
 * Domain contract for all authentication operations.
 *
 * Implementations live in the data layer. The domain layer only depends on this
 * interface — never on concrete Room/SharedPreferences types.
 */
interface AuthRepository {

    /**
     * Creates a new coach account on this device.
     *
     * @param firstName The coach's given name (trimmed, non-blank, ≤50 chars).
     * @param lastName  The coach's family name (trimmed, non-blank, ≤50 chars).
     * @param username  The desired login handle — normalized to lowercase+trim.
     * @param password  Plain-text password; hashed inside the data layer via PBKDF2.
     * @param securityQuestion A memorable question used for password reset.
     * @param securityAnswer   The answer; stored as a normalized hash (trim+lowercase).
     * @return [AuthResult.Success] with the new [User], or a typed [AuthResult.Failure].
     */
    suspend fun signUp(
        firstName: String,
        lastName: String,
        username: String,
        password: String,
        securityQuestion: String,
        securityAnswer: String
    ): AuthResult

    /**
     * Validates credentials and establishes a session on success.
     *
     * On success, the implementation must persist the user ID via [SessionManager]
     * so that [observeCurrentUser] immediately reflects the signed-in user.
     *
     * @return [AuthResult.Success] with the existing [User], or [AuthResult.Failure]
     *         with [com.example.alearning.domain.model.auth.AuthError.InvalidCredentials].
     */
    suspend fun signIn(username: String, password: String): AuthResult

    /**
     * Clears the active session. Does not delete the account.
     */
    suspend fun signOut()

    /**
     * Reactively observes the currently signed-in user.
     *
     * Emits `null` when no session is active or after [signOut].
     */
    fun observeCurrentUser(): Flow<User?>

    /**
     * Returns `true` if the given username is already registered on this device.
     *
     * Username is normalized (trim + lowercase) before the lookup.
     */
    suspend fun isUsernameTaken(username: String): Boolean

    /**
     * Returns the total number of coach accounts stored on this device.
     *
     * Used by the auth gate to decide the initial destination: zero accounts → SignUp.
     */
    suspend fun userCount(): Int

    /**
     * Two-factor-style password reset using the security question mechanism.
     *
     * The implementation must:
     *  1. Look up the account by [username].
     *  2. Normalize and hash [securityAnswer] and compare with the stored hash.
     *  3. Hash [newPassword] and update the stored credentials.
     *
     * @return [AuthResult.Success] on update, [AuthResult.Failure] on mismatch/not-found.
     */
    suspend fun resetPassword(
        username: String,
        securityAnswer: String,
        newPassword: String
    ): AuthResult

    /**
     * Fetches the security question for [username] without revealing any credentials.
     *
     * @return The question string, or `null` if the username is not registered.
     */
    suspend fun getSecurityQuestion(username: String): String?
}
