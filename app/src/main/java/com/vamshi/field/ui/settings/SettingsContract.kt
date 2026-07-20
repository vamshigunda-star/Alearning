package com.vamshi.field.ui.settings

sealed interface SettingsAction {
    object ConnectDrive : SettingsAction
    data class ConnectDriveSuccess(val accountName: String) : SettingsAction
    data class ConnectDriveError(val error: String) : SettingsAction
    object DisconnectDrive : SettingsAction
    object BackupNow : SettingsAction
    object RequestRestoreData : SettingsAction   // button tap — opens confirmation
    object DismissRestoreConfirmation : SettingsAction
    object RestoreData : SettingsAction           // now only fired by dialog confirm
    object NavigateBack : SettingsAction
}

data class SettingsUiState(
    val isSyncing: Boolean = false,
    val isDriveConnected: Boolean = false,
    val connectedEmail: String? = null,
    val lastBackupTimestamp: Long? = null,
    val errorMessage: String? = null,
    val showRestoreConfirmation: Boolean = false,
)
