package com.vamshi.field.ui.auth.signup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vamshi.field.domain.model.auth.AuthError
import com.vamshi.field.domain.model.auth.AuthResult
import com.vamshi.field.domain.usecase.auth.SignUpUseCase
import com.vamshi.field.domain.usecase.auth.ValidateNameUseCase
import com.vamshi.field.domain.usecase.auth.ValidatePasswordUseCase
import com.vamshi.field.domain.usecase.auth.ValidateUsernameUseCase
import com.vamshi.field.domain.usecase.auth.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
    private val validateName: ValidateNameUseCase,
    private val validateUsername: ValidateUsernameUseCase,
    private val validatePassword: ValidatePasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onAction(action: SignUpAction) {
        when (action) {
            is SignUpAction.FirstNameChanged -> {
                val error = (validateName(action.value) as? ValidationResult.Invalid)?.reason
                _uiState.update { it.copy(firstName = action.value, firstNameError = error) }
            }
            is SignUpAction.LastNameChanged -> {
                val error = (validateName(action.value) as? ValidationResult.Invalid)?.reason
                _uiState.update { it.copy(lastName = action.value, lastNameError = error) }
            }
            is SignUpAction.UsernameChanged -> {
                val error = (validateUsername(action.value) as? ValidationResult.Invalid)?.reason
                _uiState.update { it.copy(username = action.value, usernameError = error) }
            }
            is SignUpAction.PasswordChanged -> {
                val error = (validatePassword(action.value) as? ValidationResult.Invalid)?.reason
                _uiState.update { it.copy(password = action.value, passwordError = error) }
            }
            is SignUpAction.SecurityQuestionChanged -> {
                val error = if (action.value.isBlank()) "Security question cannot be blank" else null
                _uiState.update {
                    it.copy(securityQuestion = action.value, securityQuestionError = error)
                }
            }
            is SignUpAction.SecurityAnswerChanged -> {
                val error = if (action.value.isBlank()) "Security answer cannot be blank" else null
                _uiState.update {
                    it.copy(securityAnswer = action.value, securityAnswerError = error)
                }
            }
            SignUpAction.TogglePasswordVisibility -> {
                _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
            }
            SignUpAction.Submit -> submit()
            SignUpAction.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
            SignUpAction.NavigationConsumed -> {
                _uiState.update { it.copy(signUpSuccess = false) }
            }
        }
    }

    private fun submit() {
        val state = _uiState.value

        // Run all field validations synchronously first.
        val firstNameError = (validateName(state.firstName) as? ValidationResult.Invalid)?.reason
        val lastNameError = (validateName(state.lastName) as? ValidationResult.Invalid)?.reason
        val usernameError = (validateUsername(state.username) as? ValidationResult.Invalid)?.reason
        val passwordError = (validatePassword(state.password) as? ValidationResult.Invalid)?.reason
        val securityQuestionError =
            if (state.securityQuestion.isBlank()) "Security question cannot be blank" else null
        val securityAnswerError =
            if (state.securityAnswer.isBlank()) "Security answer cannot be blank" else null

        val hasErrors = listOf(
            firstNameError, lastNameError, usernameError,
            passwordError, securityQuestionError, securityAnswerError
        ).any { it != null }

        _uiState.update {
            it.copy(
                firstNameError = firstNameError,
                lastNameError = lastNameError,
                usernameError = usernameError,
                passwordError = passwordError,
                securityQuestionError = securityQuestionError,
                securityAnswerError = securityAnswerError
            )
        }

        if (hasErrors) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = signUpUseCase(
                firstName = state.firstName,
                lastName = state.lastName,
                username = state.username,
                password = state.password,
                securityQuestion = state.securityQuestion,
                securityAnswer = state.securityAnswer
            )
            when (result) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, signUpSuccess = true) }
                }
                is AuthResult.Failure -> {
                    val message = result.error.toUserMessage()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = message,
                            // Surface username-taken as a field error too.
                            usernameError = if (result.error == AuthError.UsernameTaken)
                                "This username is already taken" else it.usernameError
                        )
                    }
                }
            }
        }
    }
}

private fun AuthError.toUserMessage(): String = when (this) {
    AuthError.UsernameTaken -> "That username is already registered on this device."
    AuthError.WeakPassword -> "Password does not meet the requirements."
    AuthError.InvalidUsername -> "Username format is invalid."
    AuthError.InvalidName -> "Please check your name fields."
    AuthError.NoSecurityQuestion -> "Please provide a security question."
    else -> "Something went wrong. Please try again."
}
