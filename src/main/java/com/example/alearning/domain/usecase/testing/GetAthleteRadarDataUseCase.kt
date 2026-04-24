package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.model.standards.RadarAxis
import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class RadarAxisScore(
    val axis: RadarAxis,
    val normalizedScore: Float,   // 0f to 1f, derived from average percentile
    val testCount: Int
)

data class AthleteRadarData(
    val individualId: String,
    val axisScores: List<RadarAxisScore>
)

class GetAthleteRadarDataUseCase @Inject constructor(
    private val testingRepository: TestingRepository,
    private val standardsRepository: StandardsRepository
) {
    suspend operator fun invoke(individualId: String): AthleteRadarData {
        val latestResults = testingRepository
            .getLatestResultPerTestForIndividual(individualId)

        val categories = standardsRepository.getAllCategories().first()
        val categoryMap = categories.associateBy { it.id }

        val byAxis = mutableMapOf<RadarAxis, MutableList<Int>>()

        for (result in latestResults) {
            val test = standardsRepository.getTestById(result.testId) ?: continue
            val category = categoryMap[test.categoryId] ?: continue
            val axis = category.radarAxis ?: continue
            val percentile = result.percentile ?: continue
            byAxis.getOrPut(axis) { mutableListOf() }.add(percentile)
        }

        val axisScores = byAxis.map { (axis, percentiles) ->
            RadarAxisScore(
                axis = axis,
                normalizedScore = percentiles.average().toFloat() / 100f,
                testCount = percentiles.size
            )
        }

        return AthleteRadarData(
            individualId = individualId,
            axisScores = axisScores
        )
    }
}
