package com.example.alearning.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

@HiltWorker
class DriveBackupWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val backupFile = File(appContext.cacheDir, "alearning_backup_staged.json")
            if (!backupFile.exists()) {
                return@withContext Result.failure()
            }

            // In a complete implementation, the user's selected account name is retrieved from SharedPreferences.
            // For now, we simulate fetching the account name.
            val accountName = getSavedAccountName() ?: return@withContext Result.failure()

            val credential = GoogleAccountCredential.usingOAuth2(
                appContext, listOf(DriveScopes.DRIVE_APPDATA)
            )
            credential.selectedAccountName = accountName

            val driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            )
            .setApplicationName("ALearning Backup")
            .build()

            // Unique device id ensures we don't overwrite other devices
            val deviceId = getDeviceId()
            val fileName = "alearning_backup_$deviceId.json"

            // 1. Search if file already exists in appDataFolder
            val query = "name = '$fileName' and 'appDataFolder' in parents"
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ(query)
                .setFields("files(id)")
                .execute()

            val existingFileId = fileList.files.firstOrNull()?.id

            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = fileName
                parents = listOf("appDataFolder")
            }

            val mediaContent = FileContent("application/json", backupFile)

            if (existingFileId != null) {
                // Update existing file
                driveService.files().update(existingFileId, null, mediaContent)
                    .execute()
            } else {
                // Create new file
                driveService.files().create(fileMetadata, mediaContent)
                    .execute()
            }

            // Backup successful, optionally delete local staged file
            backupFile.delete()

            Result.success()
        } catch (e: IOException) {
            e.printStackTrace()
            // Retry on network/IO issues
            Result.retry()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun getSavedAccountName(): String? {
        // Normally retrieved from DataStore or SharedPreferences after Google Sign In
        val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("google_account_name", "dummy@gmail.com") 
    }

    private fun getDeviceId(): String {
        // Simplified device ID logic for demonstration
        val prefs = appContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id == null) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", id).apply()
        }
        return id
    }
}
