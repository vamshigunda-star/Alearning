package com.example.alearning.domain.usecase.testing

import com.example.alearning.data.local.entities.testing.TestResultEntity
import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.domain.usecase.standards.CalculatePercentileUseCase
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
        sex: String
    ): TestResultEntity {
        val percentileResult = calculatePercentile(testId, rawScore, ageAtTime.toDouble(), sex)

        val result = TestResultEntity(
            eventId = eventId,
            individualId = individualId,
            testId = testId,
            rawScore = rawScore,
            ageAtTime = ageAtTime,
            percentile = percentileResult?.percentile,
            classification = percentileResult?.classification
        )
        repository.saveResult(result)
        return result
    }
}
