package com.vamshi.field.ui.auth.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vamshi.field.domain.model.auth.AuthError
import com.vamshi.field.domain.model.auth.AuthResult
import com.vamshi.field.domain.usecase.auth.OnboardingUseCase
import com.vamshi.field.domain.usecase.auth.ValidateNameUseCase
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
class OnboardingViewModel @Inject constructor(
    private val onboardingUseCase: OnboardingUseCase,
    private val validateName: ValidateNameUseCase,
    private val validatePassword: ValidatePasswordUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.CoachNameChanged -> {
                val error = (validateName(action.value) as? ValidationResult.Invalid)?.reason
                _uiState.update { it.copy(coachName = action.value, coachNameError = error) }
            }
            is OnboardingAction.PasswordChanged -> {
                val error = (validatePassword(action.value) as? ValidationResult.Invalid)?.reason
                _uiState.update { it.copy(password = action.value, passwordError = error) }
            }
            is OnboardingAction.EmailChanged -> {
                _uiState.update { it.copy(email = action.value) }
            }
            OnboardingAction.TogglePasswordVisibility -> {
                _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
            }
            OnboardingAction.Submit -> submit()
            OnboardingAction.DismissError -> {
                _uiState.update { it.copy(errorMessage = null) }
            }
            OnboardingAction.NavigationConsumed -> {
                _uiState.update { it.copy(onboardingSuccess = false) }
            }
        }
    }

    private fun submit() {
        val state = _uiState.value

        val coachNameError = (validateName(state.coachName) as? ValidationResult.Invalid)?.reason
        val passwordError = (validatePassword(state.password) as? ValidationResult.Invalid)?.reason

        _uiState.update {
            it.copy(coachNameError = coachNameError, passwordError = passwordError)
        }

        if (coachNameError != null || passwordError != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = onboardingUseCase(
                coachName = state.coachName,
                password = state.password,
                email = state.email
            )
            when (result) {
                is AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, onboardingSuccess = true) }
                }
                is AuthResult.Failure -> {
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = result.error.toUserMessage())
                    }
                }
            }
        }
    }
}

private fun AuthError.toUserMessage(): String = when (this) {
    AuthError.WeakPassword -> "Password does not meet the requirements."
    AuthError.InvalidName -> "Please enter your name."
    AuthError.UsernameTaken -> "Something went wrong setting up your account. Please try again."
    else -> "Something went wrong. Please try again."
}
