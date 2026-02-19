package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class RemediationFlag(
    val individualId: String,
    val athleteName: String,
    val testId: String,
    val testName: String,
    val categoryName: String,
    val rawScore: Double,
    val unit: String,
    val percentile: Int,
    val classification: String?
)

data class RemediationList(
    val eventId: String,
    val groupId: String,
    val thresholdPercentile: Int,
    val flags: List<RemediationFlag>
)

class GetRemediationListUseCase @Inject constructor(
    private val testingRepository: TestingRepository,
    private val peopleRepository: PeopleRepository,
    private val standardsRepository: StandardsRepository
) {
    suspend operator fun invoke(
        eventId: String,
        groupId: String,
        thresholdPercentile: Int = 30
    ): RemediationList? {
        val individuals = peopleRepository.getIndividualsInGroup(groupId).first()
        if (individuals.isEmpty()) return null

        val individualIds = individuals.map { it.id }.toSet()
        val nameMap = individuals.associate { it.id to it.fullName }

        val categories = standardsRepository.getAllCategories().first()
        val categoryMap = categories.associateBy { it.id }

        val allEventResults = testingRepository.getEventResults(eventId).first()
        val groupResults = allEventResults.filter { it.individualId in individualIds }

        val flagged = groupResults.filter { it.percentile != null && it.percentile <= thresholdPercentile }

        val flags = mutableListOf<RemediationFlag>()
        for (result in flagged) {
            val test = standardsRepository.getTestById(result.testId) ?: continue
            val category = categoryMap[test.categoryId]
            flags.add(
                RemediationFlag(
                    individualId = result.individualId,
                    athleteName = nameMap[result.individualId] ?: "",
                    testId = result.testId,
                    testName = test.name,
                    categoryName = category?.name ?: "",
                    rawScore = result.rawScore,
                    unit = test.unit,
                    percentile = result.percentile!!,
                    classification = result.classification
                )
            )
        }

        return RemediationList(
            eventId = eventId,
            groupId = groupId,
            thresholdPercentile = thresholdPercentile,
            flags = flags.sortedBy { it.percentile }
        )
    }
}
