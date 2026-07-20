package com.vamshi.field.domain.usecase.backup

import com.vamshi.field.domain.repository.BackupRepository
import javax.inject.Inject

class RestoreDataUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke() {
        backupRepository.restoreFromDrive()
    }
}
