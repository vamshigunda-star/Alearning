package com.example.alearning.ui.settings

sealed interface SettingsAction {
    object ConnectDrive : SettingsAction
    data class ConnectDriveSuccess(val accountName: String) : SettingsAction
    data class ConnectDriveError(val error: String) : SettingsAction
    object BackupNow : SettingsAction
    object RestoreData : SettingsAction
    object NavigateBack : SettingsAction
}

data class SettingsUiState(
    val isSyncing: Boolean = false,
    val isDriveConnected: Boolean = false,
    val lastBackupTimestamp: Long? = null,
    val errorMessage: String? = null,
)
