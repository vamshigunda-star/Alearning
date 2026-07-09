package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.model.testing.PendingTestEntry
import com.example.alearning.domain.repository.PendingTestEntryRepository
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
