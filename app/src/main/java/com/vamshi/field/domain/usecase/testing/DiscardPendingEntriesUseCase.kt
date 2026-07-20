package com.vamshi.field.domain.usecase.testing

import com.vamshi.field.domain.repository.PendingTestEntryRepository
import javax.inject.Inject

class DiscardPendingEntriesUseCase @Inject constructor(
    private val repository: PendingTestEntryRepository
) {
    suspend operator fun invoke(eventId: String) {
        repository.discardAllForEvent(eventId)
    }
}
