package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

data class GroupProgressPoint(
    val date: Long,
    val averagePercentile: Float,
    val athleteCount: Int
)

data class GroupProgress(
    val groupId: String,
    val testId: String,
    val testName: String,
    val dataPoints: List<GroupProgressPoint>
)

class GetGroupProgressUseCase @Inject constructor(
    private val testingRepository: TestingRepository,
    private val peopleRepository: PeopleRepository,
    private val standardsRepository: StandardsRepository
) {
    suspend operator fun invoke(
        groupId: String,
        testId: String
    ): GroupProgress? {
        val test = standardsRepository.getTestById(testId) ?: return null
        
        val groupMembers = peopleRepository.getIndividualsInGroup(groupId).first()
        val memberIds = groupMembers.map { it.id }.toSet()
        
        // Get all results for this group and test
        val allResults = testingRepository.getAllEvents().first().flatMap { event ->
            testingRepository.getEventResults(event.id).first()
                .filter { it.testId == testId && it.individualId in memberIds }
                .map { it to event.date }
        }

        if (allResults.isEmpty()) return null

        // Group by date (event date)
        val dataPoints = allResults.groupBy { it.second }
            .map { (date, resultsWithDate) ->
                val results = resultsWithDate.map { it.first }
                val validPercentiles = results.mapNotNull { it.percentile }
                GroupProgressPoint(
                    date = date,
                    averagePercentile = if (validPercentiles.isNotEmpty()) validPercentiles.average().toFloat() else 0f,
                    athleteCount = results.map { it.individualId }.distinct().size
                )
            }
            .sortedBy { it.date }

        return GroupProgress(
            groupId = groupId,
            testId = testId,
            testName = test.name,
            dataPoints = dataPoints
        )
    }
}
