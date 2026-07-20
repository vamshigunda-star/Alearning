package com.vamshi.field.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vamshi.field.domain.repository.BackupRepository
import com.vamshi.field.domain.usecase.backup.BackupDataUseCase
import com.vamshi.field.domain.usecase.backup.RestoreDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.vamshi.field.data.backup.DriveBackupHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupDataUseCase: BackupDataUseCase,
    private val restoreDataUseCase: RestoreDataUseCase,
    private val backupRepository: BackupRepository,
    private val driveBackupHelper: DriveBackupHelper,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Determine initial connection state using GoogleSignIn
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val isConnected = account != null
        _uiState.update { it.copy(isDriveConnected = isConnected, connectedEmail = account?.email) }

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
            is SettingsAction.DisconnectDrive -> handleDisconnectDrive()
            is SettingsAction.BackupNow -> handleBackupNow()
            is SettingsAction.RequestRestoreData -> _uiState.update { it.copy(showRestoreConfirmation = true) }
            is SettingsAction.DismissRestoreConfirmation -> _uiState.update { it.copy(showRestoreConfirmation = false) }
            is SettingsAction.RestoreData -> handleRestoreData()
            is SettingsAction.NavigateBack -> Unit
        }
    }

    private fun handleDisconnectDrive() {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("google_account_name").apply()
        _uiState.update { it.copy(isDriveConnected = false, connectedEmail = null, errorMessage = null) }
        viewModelScope.launch {
            try {
                driveBackupHelper.signOut(context)
            } catch (e: Exception) {
                // Ignore sign out errors
            }
        }
    }

    private fun handleConnectDriveSuccess(accountName: String) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("google_account_name", accountName).apply()
        _uiState.update { it.copy(isDriveConnected = true, connectedEmail = accountName, errorMessage = null) }
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
        _uiState.update { it.copy(showRestoreConfirmation = false) }
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
