package com.example.alearning.interpretation

import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.model.testing.TestResult
import com.example.alearning.domain.repository.StandardsRepository
import javax.inject.Inject
import javax.inject.Singleton

enum class Classification { SUPERIOR, HEALTHY, NEEDS_IMPROVEMENT, NO_DATA }

data class NormResult(
    val percentile: Int,
    val classification: Classification,
    val classificationLabel: String?
)

data class Distribution(
    val superior: Int,
    val healthy: Int,
    val needsImprovement: Int,
    val noData: Int
) {
    val total: Int get() = superior + healthy + needsImprovement + noData
}

data class AthleteFlag(
    val individualId: String,
    val athleteName: String,
    val groupId: String,
    val groupName: String,
    val type: FlagType,
    val message: String,
    val testId: String? = null,
    val testName: String? = null,
    val testIds: List<String> = emptyList(),
    val testNames: List<String> = emptyList()
)

enum class FlagType { BELOW_HEALTHY, REGRESSION, ABSENT, MISSING_DATA }

@Singleton
class InterpretationEngine @Inject constructor(
    private val standards: StandardsRepository
) {

    fun classify(percentile: Int?): Classification = when {
        percentile == null -> Classification.NO_DATA
        percentile >= 70 -> Classification.SUPERIOR
        percentile >= 35 -> Classification.HEALTHY
        else -> Classification.NEEDS_IMPROVEMENT
    }

    suspend fun lookup(
        testId: String,
        sex: BiologicalSex,
        ageYears: Double,
        rawScore: Double
    ): NormResult? {
        val norm = standards.getNormResult(testId, sex, ageYears, rawScore) ?: return null
        return NormResult(
            percentile = norm.percentile,
            classification = classify(norm.percentile),
            classificationLabel = norm.classification
        )
    }

    fun athleteSessionAvgPercentile(sessionResults: List<TestResult>): Int? {
        val pctiles = sessionResults.mapNotNull { it.percentile }
        if (pctiles.isEmpty()) return null
        return pctiles.average().toInt()
    }

    fun groupSessionDistribution(athleteSessionAvgPctiles: List<Int?>): Distribution {
        var sup = 0; var hea = 0; var ni = 0; var nd = 0
        for (p in athleteSessionAvgPctiles) {
            when (classify(p)) {
                Classification.SUPERIOR -> sup++
                Classification.HEALTHY -> hea++
                Classification.NEEDS_IMPROVEMENT -> ni++
                Classification.NO_DATA -> nd++
            }
        }
        return Distribution(sup, hea, ni, nd)
    }

    /**
     * Average percentile per session for one test, ordered by sessionDate ascending.
     * @param resultsBySessionId map of sessionId -> all results across athletes for that test
     * @param sessionDateById   map of sessionId -> session date (epoch)
     */
    fun groupTrendPerTest(
        resultsBySessionId: Map<String, List<TestResult>>,
        sessionDateById: Map<String, Long>
    ): List<Pair<Long, Float>> {
        return resultsBySessionId.entries
            .mapNotNull { (sessionId, results) ->
                val date = sessionDateById[sessionId] ?: return@mapNotNull null
                val pctiles = results.mapNotNull { it.percentile }
                if (pctiles.isEmpty()) null else date to pctiles.average().toFloat()
            }
            .sortedBy { it.first }
    }

    /**
     * Compute flags for one group given the latest session's results plus history.
     *
     * @param latestSessionResults    all TestResults from the latest session, scoped to this group's members
     * @param previousSessionResultsByAthleteAndTest history map: (athleteId, testId) -> previous attempt before latest
     * @param athletesInGroup         all current group members
     * @param expectedTestsByAthlete  map of athleteId -> set of testIds expected for the athlete (age/sex norms exist)
     */
    suspend fun computeFlags(
        groupId: String,
        groupName: String,
        latestSessionResults: List<TestResult>,
        previousSessionResultsByAthleteAndTest: Map<Pair<String, String>, TestResult>,
        athletesInGroup: List<Pair<String, String>>, // id -> name
        expectedTestsByAthlete: Map<String, Set<String>>
    ): List<AthleteFlag> {
        val flags = mutableListOf<AthleteFlag>()
        val resultsByAthlete = latestSessionResults.groupBy { it.individualId }
        val testNameCache = mutableMapOf<String, String>()

        for ((athleteId, athleteName) in athletesInGroup) {
            val mine = resultsByAthlete[athleteId].orEmpty()

            // ABSENT
            if (mine.isEmpty()) {
                flags += AthleteFlag(
                    individualId = athleteId,
                    athleteName = athleteName,
                    groupId = groupId,
                    groupName = groupName,
                    type = FlagType.ABSENT,
                    message = "Did not test in latest session"
                )
                continue
            }

            // BELOW HEALTHY (athlete-session average < 35)
            val avg = athleteSessionAvgPercentile(mine)
            if (avg != null && avg < 35) {
                flags += AthleteFlag(
                    individualId = athleteId,
                    athleteName = athleteName,
                    groupId = groupId,
                    groupName = groupName,
                    type = FlagType.BELOW_HEALTHY,
                    message = "Session avg ${avg}th — below Healthy zone"
                )
            }

            // REGRESSION (any test dropped ≥12 percentiles vs previous attempt)
            for (r in mine) {
                val prev = previousSessionResultsByAthleteAndTest[athleteId to r.testId]
                val cur = r.percentile
                val old = prev?.percentile
                if (cur != null && old != null && (old - cur) >= 12) {
                    val testName = testNameCache[r.testId] ?: run {
                        val name = standards.getTestById(r.testId)?.name ?: "Unknown Test"
                        testNameCache[r.testId] = name
                        name
                    }
                    flags += AthleteFlag(
                        individualId = athleteId,
                        athleteName = athleteName,
                        groupId = groupId,
                        groupName = groupName,
                        type = FlagType.REGRESSION,
                        message = "Dropped ${old - cur} percentiles vs last session in $testName",
                        testId = r.testId,
                        testName = testName
                    )
                }
            }

            // MISSING DATA (expected tests for age missing)
            val expected = expectedTestsByAthlete[athleteId].orEmpty()
            val taken = mine.map { it.testId }.toSet()
            val missing = expected - taken
            if (expected.isNotEmpty() && missing.isNotEmpty()) {
                val missingNames = missing.map { id ->
                    testNameCache[id] ?: run {
                        val name = standards.getTestById(id)?.name ?: "Unknown Test"
                        testNameCache[id] = name
                        name
                    }
                }
                flags += AthleteFlag(
                    individualId = athleteId,
                    athleteName = athleteName,
                    groupId = groupId,
                    groupName = groupName,
                    type = FlagType.MISSING_DATA,
                    message = "Missing ${missing.size} expected test${if (missing.size == 1) "" else "s"}",
                    testIds = missing.toList(),
                    testNames = missingNames
                )
            }
        }
        return flags
    }
}
