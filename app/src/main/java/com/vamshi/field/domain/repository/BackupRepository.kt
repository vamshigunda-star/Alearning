package com.vamshi.field.domain.repository

import com.vamshi.field.domain.model.backup.DriveBackupSummary
import kotlinx.coroutines.flow.Flow

interface BackupRepository {
    /**
     * Exports local data to JSON and uploads it to Google Drive, suspending
     * until the upload completes. Overwrites this device's own prior backup,
     * if any — other devices' backups are untouched.
     */
    suspend fun backupToDrive()

    /**
     * Lists every backup currently on Drive for the signed-in account, one per
     * device that has ever backed up, newest first.
     */
    suspend fun listAvailableBackups(): List<DriveBackupSummary>

    /**
     * Downloads the backup identified by [backupId] (from [listAvailableBackups])
     * and overwrites the local user-generated data with it.
     */
    suspend fun restoreFromDrive(backupId: String)

    /**
     * Returns the timestamp of the last successful backup (local or remote), 
     * or null if unknown.
     */
    suspend fun getLastBackupTimestamp(): Long?

    /**
     * Observes the syncing state of the background worker.
     */
    fun isSyncing(): Flow<Boolean>
}
