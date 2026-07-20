package com.vamshi.field.domain.usecase.testing

import com.vamshi.field.domain.model.testing.PendingTestEntry
import com.vamshi.field.domain.repository.PendingTestEntryRepository
import javax.inject.Inject

class StagePendingEntryUseCase @Inject constructor(
    private val repository: PendingTestEntryRepository
) {
    suspend operator fun invoke(
        eventId: String,
        individualId: String,
        testId: String,
        rawScore: Double
    ) {
        repository.upsert(
            PendingTestEntry(
                eventId = eventId,
                individualId = individualId,
                testId = testId,
                rawScore = rawScore,
                stagedAt = System.currentTimeMillis()
            )
        )
    }
}
