package com.vamshi.field.ui.auth.unlock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vamshi.field.domain.model.auth.AuthResult
import com.vamshi.field.domain.model.auth.User
import com.vamshi.field.domain.usecase.auth.GetPrimaryAccountUseCase
import com.vamshi.field.domain.usecase.auth.ListAccountsUseCase
import com.vamshi.field.domain.usecase.auth.UnlockUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnlockViewModel @Inject constructor(
    private val getPrimaryAccount: GetPrimaryAccountUseCase,
    private val listAccounts: ListAccountsUseCase,
    private val unlockUseCase: UnlockUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(UnlockUiState())
    val uiState: StateFlow<UnlockUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    fun onAction(action: UnlockAction) {
        when (action) {
            is UnlockAction.PasswordChanged ->
                _uiState.update { it.copy(password = action.value) }
            UnlockAction.TogglePasswordVisibility ->
                _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
            UnlockAction.Submit -> submit()
            is UnlockAction.AccountSelected -> selectAccount(action.userId)
            UnlockAction.DismissError ->
                _uiState.update { it.copy(errorMessage = null) }
            UnlockAction.NavigationConsumed ->
                _uiState.update { it.copy(unlockSuccess = false) }
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val accounts = listAccounts()
            val primary = getPrimaryAccount()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    accounts = accounts,
                    showAccountSwitcher = accounts.size > 1,
                    accountId = primary?.id,
                    coachDisplayName = primary?.let(::displayNameOf) ?: "Coach"
                )
            }
        }
    }

    private fun selectAccount(userId: String) {
        val account = _uiState.value.accounts.firstOrNull { it.id == userId } ?: return
        _uiState.update {
            it.copy(
                accountId = account.id,
                coachDisplayName = displayNameOf(account),
                password = "",
                errorMessage = null
            )
        }
    }

    private fun submit() {
        val state = _uiState.value
        val accountId = state.accountId
        if (accountId == null) {
            _uiState.update { it.copy(errorMessage = "No account found on this device.") }
            return
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter your password.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = unlockUseCase(accountId, state.password)) {
                is AuthResult.Success ->
                    _uiState.update { it.copy(isLoading = false, unlockSuccess = true) }
                is AuthResult.Failure ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Incorrect password. Please try again."
                        )
                    }
            }
        }
    }
}

/** Mirrors DashboardViewModel's existing greeting fallback: "First Last", or "Coach" if blank. */
private fun displayNameOf(user: User): String =
    listOf(user.firstName, user.lastName).filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { "Coach" }
