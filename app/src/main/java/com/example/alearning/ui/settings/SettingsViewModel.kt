package com.example.alearning.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alearning.domain.repository.BackupRepository
import com.example.alearning.domain.usecase.backup.BackupDataUseCase
import com.example.alearning.domain.usecase.backup.RestoreDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupDataUseCase: BackupDataUseCase,
    private val restoreDataUseCase: RestoreDataUseCase,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Determine initial connection state (simplified)
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isConnected = prefs.contains("google_account_name")
        _uiState.update { it.copy(isDriveConnected = isConnected) }

        viewModelScope.launch {
            val timestamp = backupRepository.getLastBackupTimestamp()
            _uiState.update { it.copy(lastBackupTimestamp = timestamp) }
        }

        viewModelScope.launch {
            backupRepository.isSyncing().collect { syncing ->
                _uiState.update { it.copy(isSyncing = syncing) }
            }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.ConnectDrive -> { /* handled in UI */ }
            is SettingsAction.ConnectDriveSuccess -> handleConnectDriveSuccess(action.accountName)
            is SettingsAction.ConnectDriveError -> _uiState.update { it.copy(errorMessage = action.error) }
            is SettingsAction.BackupNow -> handleBackupNow()
            is SettingsAction.RestoreData -> handleRestoreData()
            is SettingsAction.NavigateBack -> Unit
        }
    }

    private fun handleConnectDriveSuccess(accountName: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("google_account_name", accountName).apply()
        _uiState.update { it.copy(isDriveConnected = true, errorMessage = null) }
    }

    private fun handleBackupNow() {
        if (!_uiState.value.isDriveConnected) {
            _uiState.update { it.copy(errorMessage = "Please connect to Google Drive first.") }
            return
        }

        viewModelScope.launch {
            try {
                backupDataUseCase()
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to backup") }
            }
        }
    }

    private fun handleRestoreData() {
        if (!_uiState.value.isDriveConnected) {
            _uiState.update { it.copy(errorMessage = "Please connect to Google Drive first.") }
            return
        }

        viewModelScope.launch {
            try {
                restoreDataUseCase()
                val timestamp = backupRepository.getLastBackupTimestamp()
                _uiState.update { it.copy(lastBackupTimestamp = timestamp, errorMessage = "Restore successful") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.message ?: "Failed to restore") }
            }
        }
    }
}
