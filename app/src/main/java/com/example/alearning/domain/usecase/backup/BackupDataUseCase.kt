package com.example.alearning.domain.usecase.backup

import com.example.alearning.domain.repository.BackupRepository
import javax.inject.Inject

class BackupDataUseCase @Inject constructor(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke() {
        backupRepository.backupToDrive()
    }
}
