package com.vamshi.field.ui.auth.restore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vamshi.field.domain.model.auth.AuthResult
import com.vamshi.field.domain.usecase.auth.CompleteRestoreUseCase
import com.vamshi.field.domain.usecase.backup.RestoreDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RestoreBackupViewModel @Inject constructor(
    private val restoreDataUseCase: RestoreDataUseCase,
    private val completeRestoreUseCase: CompleteRestoreUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RestoreBackupUiState())
    val uiState: StateFlow<RestoreBackupUiState> = _uiState.asStateFlow()

    fun onAction(action: RestoreBackupAction) {
        when (action) {
            RestoreBackupAction.GoogleSignInSucceeded -> restore()
            is RestoreBackupAction.GoogleSignInFailed ->
                _uiState.update { it.copy(errorMessage = action.message) }
            RestoreBackupAction.DismissError ->
                _uiState.update { it.copy(errorMessage = null) }
            RestoreBackupAction.NavigationConsumed ->
                _uiState.update { it.copy(restoreSuccess = false) }
        }
    }

    private fun restore() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                restoreDataUseCase()
                when (val result = completeRestoreUseCase()) {
                    is AuthResult.Success ->
                        _uiState.update { it.copy(isLoading = false, restoreSuccess = true) }
                    is AuthResult.Failure ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = "The backup didn't contain a valid account. Please try again."
                            )
                        }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Restore failed. Please try again.")
                }
            }
        }
    }
}
