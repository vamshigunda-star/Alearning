package com.example.alearning.ui.auth.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.model.auth.AuthResult
import com.example.alearning.domain.usecase.auth.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.UsernameChanged ->
                _uiState.update { it.copy(username = action.value) }
            is SignInAction.PasswordChanged ->
                _uiState.update { it.copy(password = action.value) }
            SignInAction.TogglePasswordVisibility ->
                _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
            SignInAction.Submit -> submit()
            SignInAction.ResetPasswordClicked ->
                _uiState.update { it.copy(navigateToReset = true) }
            SignInAction.NavigateToSignUpClicked ->
                _uiState.update { it.copy(navigateToSignUp = true) }
            SignInAction.DismissError ->
                _uiState.update { it.copy(errorMessage = null) }
            SignInAction.NavigationConsumed ->
                _uiState.update {
                    it.copy(
                        signInSuccess = false,
                        navigateToReset = false,
                        navigateToSignUp = false
                    )
                }
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.username.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your username and password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = signInUseCase(state.username, state.password)) {
                is AuthResult.Success ->
                    _uiState.update { it.copy(isLoading = false, signInSuccess = true) }
                is AuthResult.Failure ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Invalid username or password. Please try again."
                        )
                    }
            }
        }
    }
}
