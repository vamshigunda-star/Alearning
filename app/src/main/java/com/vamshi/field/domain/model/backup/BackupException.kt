package com.vamshi.field.domain.model.backup

/**
 * User-facing backup/restore failure reasons. [DriveBackupHelper][com.vamshi.field.data.backup.DriveBackupHelper]
 * and [BackupRepositoryImpl][com.vamshi.field.data.repository.BackupRepositoryImpl] translate SDK-level
 * exceptions (IOException, GoogleJsonResponseException, UserRecoverableAuthIOException, ...) into these so
 * the UI layer can display [message] directly instead of raw SDK text.
 */
sealed class BackupException(message: String) : Exception(message) {
    object NotSignedIn : BackupException("Please connect to Google Drive first.")
    object NoBackupFound : BackupException("No backup was found on Google Drive for this account.")
    object NoNetwork : BackupException("No internet connection. Check your connection and try again.")
    object ReauthRequired : BackupException("Google Drive access needs to be reconnected. Please sign in again.")
    object StorageQuotaExceeded : BackupException("Your Google Drive storage is full. Free up space and try again.")
    object CorruptedBackup : BackupException("The backup file is invalid or corrupted and can't be restored.")
    data class Unknown(val original: Throwable) :
        BackupException(original.message ?: "Something went wrong. Please try again.")
}
