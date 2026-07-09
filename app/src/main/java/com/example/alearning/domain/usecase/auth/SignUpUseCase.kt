package com.example.alearning.domain.usecase.auth

import com.example.alearning.domain.model.auth.AuthError
import com.example.alearning.domain.model.auth.AuthResult
import com.example.alearning.domain.repository.AuthRepository
import javax.inject.Inject

/**
 * Creates a new coach account on this device.
 *
 * Validation order:
 *  1. First name
 *  2. Last name
 *  3. Username format
 *  4. Password strength
 *  5. Security question non-blank
 *  6. Security answer non-blank
 *  7. Username uniqueness (requires DB lookup — done last to minimize IO on invalid input)
 *
 * On success: the data layer hashes the password and security answer, stores the
 * account, and sets the session so the user is immediately signed in.
 */
class SignUpUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val validateName: ValidateNameUseCase,
    private val validateUsername: ValidateUsernameUseCase,
    private val validatePassword: ValidatePasswordUseCase
) {
    suspend operator fun invoke(
        firstName: String,
        lastName: String,
        username: String,
        password: String,
        securityQuestion: String,
        securityAnswer: String
    ): AuthResult {
        val firstNameResult = validateName(firstName)
        if (firstNameResult is ValidationResult.Invalid) {
            return AuthResult.Failure(AuthError.InvalidName)
        }
        val lastNameResult = validateName(lastName)
        if (lastNameResult is ValidationResult.Invalid) {
            return AuthResult.Failure(AuthError.InvalidName)
        }
        val usernameResult = validateUsername(username)
        if (usernameResult is ValidationResult.Invalid) {
            return AuthResult.Failure(AuthError.InvalidUsername)
        }
        val passwordResult = validatePassword(password)
        if (passwordResult is ValidationResult.Invalid) {
            return AuthResult.Failure(AuthError.WeakPassword)
        }
        if (securityQuestion.isBlank()) {
            return AuthResult.Failure(AuthError.NoSecurityQuestion)
        }
        if (securityAnswer.isBlank()) {
            return AuthResult.Failure(AuthError.InvalidCredentials)
        }

        return repository.signUp(
            firstName = firstName,
            lastName = lastName,
            username = username,
            password = password,
            securityQuestion = securityQuestion,
            securityAnswer = securityAnswer
        )
    }
}
