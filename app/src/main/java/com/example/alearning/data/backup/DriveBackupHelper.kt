package com.example.alearning.data.backup

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class DriveBackupHelper @Inject constructor() {

    private val BACKUP_FILE_NAME = "alearning_backup.json"
    private val BACKUP_FILE_MIME_TYPE = "application/json"

    private fun getDriveService(context: Context): Drive {
        val account = GoogleSignIn.getLastSignedInAccount(context)?.account
            ?: throw Exception("No Google account signed in")

        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_APPDATA)
        ).apply {
            selectedAccount = account
        }

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
        .setApplicationName("ALearning Backup")
        .build()
    }

    suspend fun uploadToDrive(context: Context, localBackupFile: File) = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context)

        // Check if file already exists in appDataFolder
        val fileList = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name='$BACKUP_FILE_NAME'")
            .setFields("files(id, name)")
            .execute()

        val existingFile = fileList.files.firstOrNull()

        val fileMetadata = com.google.api.services.drive.model.File().apply {
            name = BACKUP_FILE_NAME
            mimeType = BACKUP_FILE_MIME_TYPE
            parents = listOf("appDataFolder")
        }

        val fileContent = FileContent(BACKUP_FILE_MIME_TYPE, localBackupFile)

        if (existingFile != null) {
            // Update existing file
            fileMetadata.parents = null // Cannot change parents on update
            driveService.files().update(existingFile.id, fileMetadata, fileContent).execute()
        } else {
            // Create new file
            driveService.files().create(fileMetadata, fileContent).execute()
        }
    }

    suspend fun downloadFromDrive(context: Context, targetLocalFile: File) = withContext(Dispatchers.IO) {
        val driveService = getDriveService(context)

        val fileList = driveService.files().list()
            .setSpaces("appDataFolder")
            .setQ("name='$BACKUP_FILE_NAME'")
            .setFields("files(id, name)")
            .execute()

        val existingFile = fileList.files.firstOrNull()
            ?: throw Exception("No backup found on Google Drive")

        FileOutputStream(targetLocalFile).use { outputStream ->
            driveService.files().get(existingFile.id).executeMediaAndDownloadTo(outputStream)
        }
    }
}
