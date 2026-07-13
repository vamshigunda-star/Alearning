package com.example.alearning.domain.repository

import kotlinx.coroutines.flow.Flow

interface BackupRepository {
    /**
     * Triggers a local data export to JSON and enqueues a background 
     * Worker to sync to Google Drive.
     */
    suspend fun backupToDrive()

    /**
     * Authenticates with Google Drive, downloads the latest backup file,
     * and overwrites the local user-generated data.
     */
    suspend fun restoreFromDrive()

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
