package com.vamshi.field.domain.usecase.testing

import com.vamshi.field.domain.model.testing.PendingTestEntry
import com.vamshi.field.domain.repository.PendingTestEntryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePendingEntriesUseCase @Inject constructor(
    private val repository: PendingTestEntryRepository
) {
    operator fun invoke(eventId: String): Flow<List<PendingTestEntry>> =
        repository.observePendingForEvent(eventId)
}
