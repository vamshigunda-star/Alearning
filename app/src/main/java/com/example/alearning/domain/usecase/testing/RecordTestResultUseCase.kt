package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.model.testing.TestResult
import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.domain.usecase.standards.CalculatePercentileUseCase
import java.util.UUID
import javax.inject.Inject

class RecordTestResultUseCase @Inject constructor(
    private val repository: TestingRepository,
    private val calculatePercentile: CalculatePercentileUseCase
) {
    suspend operator fun invoke(
        eventId: String,
        individualId: String,
        testId: String,
        rawScore: Double,
        ageAtTime: Float,
        sex: BiologicalSex
    ): TestResult {
        val percentileResult = calculatePercentile(testId, rawScore, ageAtTime.toDouble(), sex)

        val result = TestResult(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            individualId = individualId,
            testId = testId,
            rawScore = rawScore,
            ageAtTime = ageAtTime,
            percentile = percentileResult?.percentile,
            classification = percentileResult?.classification,
            createdAt = System.currentTimeMillis()
        )
        repository.saveResult(result)
        return result
    }
}
