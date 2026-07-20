package com.vamshi.field.ui.auth.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vamshi.field.domain.model.auth.AuthResult
import com.vamshi.field.domain.usecase.auth.GetSecurityQuestionUseCase
import com.vamshi.field.domain.usecase.auth.ResetPasswordUseCase
import com.vamshi.field.domain.usecase.auth.ValidatePasswordUseCase
import com.vamshi.field.domain.usecase.auth.ValidationResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val getSecurityQuestion: GetSecurityQuestionUseCase,
    private val resetPassword: ResetPasswordUseCase,
    private val validatePassword: ValidatePasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState.asStateFlow()

    fun onAction(action: ResetPasswordAction) {
        when (action) {
            is ResetPasswordAction.UsernameChanged ->
                _uiState.update { it.copy(username = action.value) }
            ResetPasswordAction.ContinueClicked -> loadSecurityQuestion()
            is ResetPasswordAction.AnswerChanged -> {
                val error = if (action.value.isBlank()) "Answer cannot be blank" else null
                _uiState.update { it.copy(securityAnswer = action.value, securityAnswerError = error) }
            }
            is ResetPasswordAction.NewPasswordChanged -> {
                val error = (validatePassword(action.value) as? ValidationResult.Invalid)?.reason
                _uiState.update { it.copy(newPassword = action.value, newPasswordError = error) }
            }
            is ResetPasswordAction.ConfirmPasswordChanged -> {
                val error = if (action.value != _uiState.value.newPassword)
                    "Passwords do not match" else null
                _uiState.update {
                    it.copy(confirmPassword = action.value, confirmPasswordError = error)
                }
            }
            ResetPasswordAction.ToggleNewPasswordVisibility ->
                _uiState.update { it.copy(newPasswordVisible = !it.newPasswordVisible) }
            ResetPasswordAction.Submit -> submit()
            ResetPasswordAction.BackClicked ->
                _uiState.update { it.copy(step = 1, errorMessage = null) }
            ResetPasswordAction.DismissError ->
                _uiState.update { it.copy(errorMessage = null) }
            ResetPasswordAction.NavigationConsumed ->
                _uiState.update { it.copy(resetSuccess = false) }
        }
    }

    private fun loadSecurityQuestion() {
        val username = _uiState.value.username.trim()
        if (username.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your username.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val question = getSecurityQuestion(username)
            if (question == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "No account found for that username."
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isLoading = false, step = 2, securityQuestion = question)
                }
            }
        }
    }

    private fun submit() {
        val state = _uiState.value

        val answerError = if (state.securityAnswer.isBlank()) "Answer cannot be blank" else null
        val passwordError =
            (validatePassword(state.newPassword) as? ValidationResult.Invalid)?.reason
        val confirmError = if (state.confirmPassword != state.newPassword)
            "Passwords do not match" else null

        _uiState.update {
            it.copy(
                securityAnswerError = answerError,
                newPasswordError = passwordError,
                confirmPasswordError = confirmError
            )
        }
        if (listOf(answerError, passwordError, confirmError).any { it != null }) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = resetPassword(
                username = state.username,
                securityAnswer = state.securityAnswer,
                newPassword = state.newPassword
            )
            when (result) {
                is AuthResult.Success ->
                    _uiState.update { it.copy(isLoading = false, resetSuccess = true) }
                is AuthResult.Failure ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Incorrect security answer. Please try again."
                        )
                    }
            }
        }
    }
}
