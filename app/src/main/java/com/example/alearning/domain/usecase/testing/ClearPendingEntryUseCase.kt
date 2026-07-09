package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.repository.PendingTestEntryRepository
import javax.inject.Inject

class ClearPendingEntryUseCase @Inject constructor(
    private val repository: PendingTestEntryRepository
) {
    suspend operator fun invoke(eventId: String, individualId: String, testId: String) {
        repository.delete(eventId, individualId, testId)
    }
}
