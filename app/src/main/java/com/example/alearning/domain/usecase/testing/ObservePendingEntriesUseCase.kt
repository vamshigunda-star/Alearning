package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.model.testing.PendingTestEntry
import com.example.alearning.domain.repository.PendingTestEntryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePendingEntriesUseCase @Inject constructor(
    private val repository: PendingTestEntryRepository
) {
    operator fun invoke(eventId: String): Flow<List<PendingTestEntry>> =
        repository.observePendingForEvent(eventId)
}
