package com.vamshi.field.domain.usecase.testing

import com.vamshi.field.domain.repository.PendingTestEntryRepository
import javax.inject.Inject

class ClearPendingEntryUseCase @Inject constructor(
    private val repository: PendingTestEntryRepository
) {
    suspend operator fun invoke(eventId: String, individualId: String, testId: String) {
        repository.delete(eventId, individualId, testId)
    }
}
