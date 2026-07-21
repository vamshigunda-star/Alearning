package com.vamshi.field.domain.usecase.backup

import com.vamshi.field.domain.model.backup.DriveBackupSummary
import com.vamshi.field.domain.repository.BackupRepository
import javax.inject.Inject

class ListAvailableBackupsUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): List<DriveBackupSummary> = backupRepository.listAvailableBackups()
}
