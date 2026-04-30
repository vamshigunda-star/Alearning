package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.model.analytics.GroupProgressPoint
import com.example.alearning.domain.model.analytics.TimePeriod
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class GetGroupProgressUseCase @Inject constructor(
    private val testingRepository: TestingRepository,
    private val peopleRepository: PeopleRepository,
    private val standardsRepository: StandardsRepository
) {
    suspend operator fun invoke(
        period: TimePeriod
    ): Map<String, List<GroupProgressPoint>> {
        val groups = peopleRepository.getAllGroups().first()
        val allEvents = testingRepository.getAllEvents().first()
        
        val cutOffDate = if (period == TimePeriod.ALL) null else LocalDate.now().minusMonths(period.months.toLong())

        val resultMap = mutableMapOf<String, List<GroupProgressPoint>>()

        for (group in groups) {
            val groupMembers = peopleRepository.getIndividualsInGroup(group.id).first()
            val memberIds = groupMembers.map { it.id }.toSet()

            val allResults = allEvents.flatMap { event ->
                val eventDate = Instant.ofEpochMilli(event.date).atZone(ZoneId.systemDefault()).toLocalDate()
                if (cutOffDate != null && eventDate.isBefore(cutOffDate)) {
                    emptyList()
                } else {
                    testingRepository.getEventResults(event.id).first()
                        .filter { it.individualId in memberIds && it.percentile != null }
                        .map { it to eventDate }
                }
            }

            if (allResults.isNotEmpty()) {
                val dataPoints = allResults.groupBy { it.second }
                    .map { (date, resultsWithDate) ->
                        val validPercentiles = resultsWithDate.map { it.first.percentile!! }
                        GroupProgressPoint(
                            date = date,
                            avgScore = validPercentiles.average().toFloat() / 100f
                        )
                    }
                    .sortedBy { it.date }
                
                if (dataPoints.isNotEmpty()) {
                    resultMap[group.name] = dataPoints
                }
            }
        }

        return resultMap
    }
}
