package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class AthleteTestSummary(
    val testId: String,
    val testName: String,
    val categoryName: String,
    val unit: String,
    val latestScore: Double,
    val latestDate: Long,
    val percentile: Int?,
    val classification: String?,
    val isHigherBetter: Boolean,
    val totalAttempts: Int
)

data class AthleteProfile(
    val individualId: String,
    val athleteName: String,
    val summaries: List<AthleteTestSummary>
)

class GetAthleteProfileUseCase @Inject constructor(
    private val peopleRepository: PeopleRepository,
    private val testingRepository: TestingRepository,
    private val standardsRepository: StandardsRepository
) {
    suspend operator fun invoke(individualId: String): AthleteProfile? {
        val individual = peopleRepository.getIndividualById(individualId) ?: return null

        val allResults = testingRepository.getAllResultsForIndividual(individualId).first()
        if (allResults.isEmpty()) {
            return AthleteProfile(
                individualId = individualId,
                athleteName = individual.fullName,
                summaries = emptyList()
            )
        }

        val categories = standardsRepository.getAllCategories().first()
        val categoryMap = categories.associateBy { it.id }

        val resultsByTestId = allResults.groupBy { it.testId }

        // Build summaries with their category sort order for sorting later
        val summariesWithSortKey = mutableListOf<Pair<AthleteTestSummary, Int>>()

        for ((testId, results) in resultsByTestId) {
            val test = standardsRepository.getTestById(testId) ?: continue
            val category = categoryMap[test.categoryId]
            val latestResult = results.maxByOrNull { it.createdAt } ?: continue

            val summary = AthleteTestSummary(
                testId = testId,
                testName = test.name,
                categoryName = category?.name ?: "",
                unit = test.unit,
                latestScore = latestResult.rawScore,
                latestDate = latestResult.createdAt,
                percentile = latestResult.percentile,
                classification = latestResult.classification,
                isHigherBetter = test.isHigherBetter,
                totalAttempts = results.size
            )
            summariesWithSortKey.add(summary to (category?.sortOrder ?: Int.MAX_VALUE))
        }

        val sortedSummaries = summariesWithSortKey
            .sortedWith(compareBy({ it.second }, { it.first.testName }))
            .map { it.first }

        return AthleteProfile(
            individualId = individualId,
            athleteName = individual.fullName,
            summaries = sortedSummaries
        )
    }
}
