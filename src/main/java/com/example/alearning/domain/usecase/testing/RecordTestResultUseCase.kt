package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.model.standards.InterpretationStrategy
import com.example.alearning.domain.model.testing.CaptureMethod
import com.example.alearning.domain.model.testing.TestResult
import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import com.example.alearning.domain.usecase.standards.CalculatePercentileUseCase
import java.util.UUID
import javax.inject.Inject

class RecordTestResultUseCase @Inject constructor(
    private val repository: TestingRepository,
    private val calculatePercentile: CalculatePercentileUseCase,
    private val standardsRepository: StandardsRepository
) {
    suspend operator fun invoke(
        eventId: String,
        individualId: String,
        testId: String,
        rawScore: Double,
        ageAtTime: Float,
        sex: BiologicalSex,
        captureMethod: CaptureMethod = CaptureMethod.MANUAL_ENTRY
    ): TestResult {
        val test = standardsRepository.getTestById(testId)

        val percentileResult = when (test?.interpretationStrategy) {
            InterpretationStrategy.NORM_LOOKUP ->
                calculatePercentile(testId, rawScore, ageAtTime.toDouble(), sex)
            InterpretationStrategy.CALCULATED ->
                null // placeholder until CalculationEngine is built
            InterpretationStrategy.NONE, null ->
                null
        }

        val result = TestResult(
            id = UUID.randomUUID().toString(),
            eventId = eventId,
            individualId = individualId,
            testId = testId,
            rawScore = rawScore,
            ageAtTime = ageAtTime,
            percentile = percentileResult?.percentile,
            classification = percentileResult?.classification,
            captureMethod = captureMethod,
            createdAt = System.currentTimeMillis()
        )
        repository.saveResult(result)
        return result
    }
}
