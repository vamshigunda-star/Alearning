package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.model.analytics.AtRiskAthlete
import com.example.alearning.domain.model.analytics.DomainPattern
import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class GetRemediationListUseCase @Inject constructor(
    private val testingRepository: TestingRepository,
    private val peopleRepository: PeopleRepository,
    private val standardsRepository: StandardsRepository
) {
    suspend fun getGlobalAtRiskAthletes(): List<AtRiskAthlete> {
        val athletes = peopleRepository.getAllIndividuals().first()
        val allGroups = peopleRepository.getAllGroups().first()
        val categories = standardsRepository.getAllCategories().first().associateBy { it.id }

        val atRiskList = mutableListOf<AtRiskAthlete>()

        for (athlete in athletes) {
            val athleteGroups = peopleRepository.getGroupsForIndividual(athlete.id).first()
            val groupName = athleteGroups.firstOrNull()?.name ?: "No Group"

            val results = testingRepository.getLatestResultPerTestForIndividual(athlete.id)
            if (results.isEmpty()) continue

            val domainScores = mutableMapOf<String, MutableList<Float>>()
            var lastTestDateLong: Long? = null

            for (result in results) {
                val test = standardsRepository.getTestById(result.testId) ?: continue
                val category = categories[test.categoryId] ?: continue
                val axis = category.radarAxis?.name ?: continue

                val score = (result.percentile ?: continue).toFloat() / 100f
                domainScores.getOrPut(axis) { mutableListOf() }.add(score)
                
                if (lastTestDateLong == null || result.createdAt > lastTestDateLong) {
                    lastTestDateLong = result.createdAt
                }
            }

            if (domainScores.isEmpty()) continue

            val overallAvg = domainScores.values.flatten().average().toFloat()
            val weakDomains = domainScores.filter { it.value.average() < 0.4f }.keys.toList()

            val lastTestDate = lastTestDateLong?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            }

            atRiskList.add(
                AtRiskAthlete(
                    athleteId = athlete.id,
                    athleteName = athlete.fullName,
                    groupName = groupName,
                    weakDomains = weakDomains,
                    avgScore = overallAvg,
                    lastTestDate = lastTestDate
                )
            )
        }

        return atRiskList
    }

    fun calculateDomainPatterns(atRiskAthletes: List<AtRiskAthlete>): List<DomainPattern> {
        val domainMap = mutableMapOf<String, MutableList<Float>>()

        for (athlete in atRiskAthletes) {
            for (domain in athlete.weakDomains) {
                domainMap.getOrPut(domain) { mutableListOf() }.add(athlete.avgScore)
            }
        }

        return domainMap.map { (domain, scores) ->
            DomainPattern(
                domainName = domain,
                atRiskCount = scores.size,
                avgScore = scores.average().toFloat(),
                isClusterPattern = scores.size >= 3
            )
        }.sortedByDescending { it.atRiskCount }
    }
}
