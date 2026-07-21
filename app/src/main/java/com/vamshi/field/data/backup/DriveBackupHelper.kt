package com.vamshi.field.data.backup

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.vamshi.field.domain.model.backup.BackupException
import com.vamshi.field.domain.model.backup.DriveBackupSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject


class DriveBackupHelper @Inject constructor(
    private val deviceIdentifier: DeviceIdentifier
) {

    private val BACKUP_FILE_NAME_PREFIX = "field_backup_"
    private val BACKUP_FILE_MIME_TYPE = "application/json"

    private fun getDriveService(context: Context): Drive {
        val account = GoogleSignIn.getLastSignedInAccount(context)?.account
            ?: throw BackupException.NotSignedIn

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
        .setApplicationName("Field Backup")
        .build()
    }

    suspend fun uploadToDrive(context: Context, localBackupFile: File) = withContext(Dispatchers.IO) {
        runCatching {
            val driveService = getDriveService(context)
            val fileName = deviceIdentifier.backupFileName

            // This device's own backup, if it already uploaded one before — re-backing-up
            // from the same device updates it in place instead of creating a duplicate.
            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name='$fileName'")
                .setFields("files(id, name)")
                .execute()

            val existingFile = fileList.files.firstOrNull()

            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = fileName
                description = deviceIdentifier.deviceLabel
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
        }.getOrElse { throw translate(it) }
    }

    /** Every backup currently on Drive for this account, newest first. */
    suspend fun listBackups(context: Context): List<DriveBackupSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val driveService = getDriveService(context)

            val fileList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name contains '$BACKUP_FILE_NAME_PREFIX'")
                .setFields("files(id, name, description, modifiedTime)")
                .execute()

            fileList.files.orEmpty().map { file ->
                DriveBackupSummary(
                    id = file.id,
                    deviceLabel = file.description?.takeIf { it.isNotBlank() } ?: file.name,
                    lastModified = file.modifiedTime?.value ?: 0L
                )
            }.sortedByDescending { it.lastModified }
        }.getOrElse { throw translate(it) }
    }

    suspend fun downloadFromDrive(context: Context, targetLocalFile: File, backupId: String) =
        withContext(Dispatchers.IO) {
            runCatching {
                val driveService = getDriveService(context)

                FileOutputStream(targetLocalFile).use { outputStream ->
                    driveService.files().get(backupId).executeMediaAndDownloadTo(outputStream)
                }
            }.getOrElse { throw translate(it) }
        }

    /** Maps SDK-level failures to a [BackupException] the UI can show directly. */
    private fun translate(error: Throwable): BackupException = when (error) {
        is BackupException -> error
        is UserRecoverableAuthIOException -> BackupException.ReauthRequired
        is GoogleJsonResponseException -> when {
            error.statusCode == 401 -> BackupException.ReauthRequired
            error.statusCode == 403 && error.details?.errors.orEmpty()
                .any { it.reason == "storageQuotaExceeded" } -> BackupException.StorageQuotaExceeded
            else -> BackupException.Unknown(error)
        }
        is IOException -> BackupException.NoNetwork
        else -> BackupException.Unknown(error)
    }

    suspend fun signOut(context: Context) = withContext(Dispatchers.IO) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        val client = GoogleSignIn.getClient(context, gso)
        client.signOut()
    }
}
