package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.standards.TimingMode
import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import javax.inject.Inject

data class StopwatchSession(
    val fitnessTest: FitnessTest,
    val athletes: List<Individual>,
    val heats: List<List<Individual>>,
    val trialsPerAthlete: Int,
    val trialCounts: Map<String, Int>
)

class StopwatchSessionUseCase @Inject constructor(
    private val testingRepository: TestingRepository,
    private val standardsRepository: StandardsRepository
) {
    suspend operator fun invoke(
        eventId: String,
        fitnessTestId: String,
        groupId: String
    ): StopwatchSession {
        val test = standardsRepository.getTestById(fitnessTestId)
            ?: throw IllegalArgumentException("Fitness test not found: $fitnessTestId")

        val athletes = testingRepository.getAthletesInGroupOrdered(groupId)

        val trialCounts = athletes.associate { athlete ->
            athlete.id to testingRepository.getTrialCountForAthlete(eventId, athlete.id, fitnessTestId)
        }

        val heats = when (test.timingMode) {
            TimingMode.GROUP_START -> {
                val perHeat = test.athletesPerHeat ?: 6
                athletes.chunked(perHeat)
            }
            TimingMode.INDIVIDUAL -> athletes.map { listOf(it) }
            TimingMode.MANUAL_ENTRY -> emptyList()
        }

        return StopwatchSession(
            fitnessTest = test,
            athletes = athletes,
            heats = heats,
            trialsPerAthlete = test.trialsPerAthlete,
            trialCounts = trialCounts
        )
    }
}
