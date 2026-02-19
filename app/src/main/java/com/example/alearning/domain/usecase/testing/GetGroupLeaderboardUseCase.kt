package com.example.alearning.domain.usecase.testing

import com.example.alearning.domain.repository.PeopleRepository
import com.example.alearning.domain.repository.StandardsRepository
import com.example.alearning.domain.repository.TestingRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

// --- Output Models ---

data class LeaderboardEntry(
    val rank: Int,
    val individualId: String,
    val athleteName: String,
    val rawScore: Double,
    val unit: String,
    val percentile: Int?,
    val classification: String?,
    val isImproved: Boolean?         // null if no previous result exists
)

data class GroupLeaderboard(
    val eventId: String,
    val testId: String,
    val testName: String,
    val isHigherBetter: Boolean,
    val entries: List<LeaderboardEntry>  // ordered rank 1 → last
)

// --- Use Case ---

class GetGroupLeaderboardUseCase @Inject constructor(
    private val testingRepository: TestingRepository,
    private val peopleRepository: PeopleRepository,
    private val standardsRepository: StandardsRepository
) {
    suspend operator fun invoke(
        eventId: String,
        testId: String,
        groupId: String
    ): GroupLeaderboard? {

        // Step 1: Fetch test metadata — needed for name, unit, isHigherBetter
        val test = standardsRepository.getTestById(testId) ?: return null

        // Step 2: Fetch all individuals in this group — used for name lookup and filtering
        val individuals = peopleRepository
            .getIndividualsInGroup(groupId)
            .first()

        // Build a quick lookup map so we don't scan the list repeatedly
        // individualId → athleteName
        val nameMap = individuals.associate { it.id to it.fullName }

        // Step 3: Fetch all results for this event
        val allEventResults = testingRepository
            .getEventResults(eventId)
            .first()

        // Step 4: Filter to only this test AND only athletes who belong to this group
        val groupAthleteIds = nameMap.keys
        val relevantResults = allEventResults.filter { result ->
            result.testId == testId && result.individualId in groupAthleteIds
        }

        // Step 5: For each result, compute isImproved by comparing to previous attempt
        val entries = relevantResults.map { result ->

            // Fetch this athlete's full history on this test
            val history = testingRepository
                .getHistoryForTest(result.individualId, testId)
                .first()

            // Find the result immediately before this event's result
            // History is ordered oldest → newest by createdAt
            val previousResult = history
                .filter { it.createdAt < result.createdAt }
                .maxByOrNull { it.createdAt }  // most recent prior attempt

            val isImproved = previousResult?.let {
                val delta = result.rawScore - it.rawScore
                if (test.isHigherBetter) delta > 0 else delta < 0
            }

            LeaderboardEntry(
                rank = 0,            // placeholder — assigned after sorting
                individualId = result.individualId,
                athleteName = nameMap[result.individualId] ?: "Unknown",
                rawScore = result.rawScore,
                unit = test.unit,
                percentile = result.percentile,
                classification = result.classification,
                isImproved = isImproved
            )
        }

        // Step 6: Sort by rawScore — direction depends on test type
        val sorted = if (test.isHigherBetter) {
            entries.sortedByDescending { it.rawScore }  // PACER: more laps = rank 1
        } else {
            entries.sortedBy { it.rawScore }             // mile run: less time = rank 1
        }

        // Step 7: Assign final rank by position (1-indexed)
        val ranked = sorted.mapIndexed { index, entry ->
            entry.copy(rank = index + 1)
        }

        return GroupLeaderboard(
            eventId = eventId,
            testId = testId,
            testName = test.name,
            isHigherBetter = test.isHigherBetter,
            entries = ranked
        )
    }
}

