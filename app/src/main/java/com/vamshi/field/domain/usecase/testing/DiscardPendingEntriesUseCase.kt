package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.repository.PendingTestEntryRepository
import javax.inject.Inject

class DiscardPendingEntriesUseCase @Inject constructor(
    private val repository: PendingTestEntryRepository
) {
    suspend operator fun invoke(eventId: String) {
        repository.discardAllForEvent(eventId)
    }
}
