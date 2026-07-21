package com.vamshi.field.domain.model.backup

/** One backup file available on Google Drive, as shown in the restore picker. */
data class DriveBackupSummary(
    val id: String,
    val deviceLabel: String,
    val lastModified: Long
)
