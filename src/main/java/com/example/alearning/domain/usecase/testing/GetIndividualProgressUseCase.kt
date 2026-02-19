package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// --- Output Models ---

data class ProgressDataPoint(
    val eventId: String,
    val date: Long,
    val rawScore: Double,
    val unit: String,
    val percentile: Int?,
    val classification: String?,
    val delta: Double?,          // null if this is the first recorded attempt
    val isImproved: Boolean?     // null if first attempt, true/false for subsequent
)

data class IndividualProgress(
    val individualId: String,
    val testId: String,
    val testName: String,
    val isHigherBetter: Boolean,
    val dataPoints: List<ProgressDataPoint>  // ordered oldest → newest
)

// --- Use Case ---

class GetIndividualProgressUseCase @Inject constructor(
    private val testingRepository: TestingRepository,
    private val standardsRepository: StandardsRepository
) {
    suspend operator fun invoke(
        individualId: String,
        testId: String,
        startDate: Long? = null,
        endDate: Long? = null
    ): IndividualProgress? {

        // Step 1: Fetch test metadata — needed for unit and isHigherBetter
        val test = standardsRepository.getTestById(testId) ?: return null

        // Step 2: Fetch full history, ordered oldest → newest (as DAO returns it)
        val history = testingRepository
            .getHistoryForTest(individualId, testId)
            .first()  // one-time fetch from Flow

        // Step 3: Apply optional date filters
        val filtered = history.filter { result ->
            val afterStart = startDate?.let { result.createdAt >= it } ?: true
            val beforeEnd = endDate?.let { result.createdAt <= it } ?: true
            afterStart && beforeEnd
        }

        // Step 4: Map each result into a ProgressDataPoint, computing delta
        val dataPoints = filtered.mapIndexed { index, result ->
            val previous = if (index == 0) null else filtered[index - 1]

            val delta = previous?.let { result.rawScore - it.rawScore }

            val isImproved = delta?.let {
                if (test.isHigherBetter) it > 0 else it < 0
            }

            ProgressDataPoint(
                eventId = result.eventId,
                date = result.createdAt,
                rawScore = result.rawScore,
                unit = test.unit,
                percentile = result.percentile,
                classification = result.classification,
                delta = delta,
                isImproved = isImproved
            )
        }

        return IndividualProgress(
            individualId = individualId,
            testId = testId,
            testName = test.name,
            isHigherBetter = test.isHigherBetter,
            dataPoints = dataPoints
        )
    }
}