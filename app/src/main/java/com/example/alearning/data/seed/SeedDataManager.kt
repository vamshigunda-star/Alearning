package com.example.alearning.data.seed

import android.content.Context
import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.data.local.entities.standards.NormReferenceEntity
import com.example.alearning.data.local.entities.standards.TestCategoryEntity
import com.example.alearning.data.mapper.standards.toDomain
import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.usecase.standards.ImportStandardsUseCase
import com.example.alearning.data.local.entities.people.IndividualEntity
import com.example.alearning.data.local.entities.people.GroupEntity
import com.example.alearning.data.local.entities.people.GroupMemberCrossRef
import com.example.alearning.data.local.entities.testing.TestingEventEntity
import com.example.alearning.data.local.entities.testing.TestResultEntity
import com.example.alearning.data.local.entities.testing.EventTestCrossRef
import java.util.Calendar
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedDataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importStandardsUseCase: ImportStandardsUseCase,
    private val database: com.example.alearning.data.AppDatabase
) {
    companion object {
        private const val PREFS_NAME = "alearning_prefs"
        private const val KEY_DATA_SEEDED = "data_seeded_csv_v11" // Bumped for reports hub
    }

    suspend fun seedIfNeeded() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DATA_SEEDED, false)) {
            android.util.Log.d("SeedDataManager", "Data already seeded")
            return
        }

        android.util.Log.d("SeedDataManager", "Starting data seeding from CSV...")
        // 1. Seed Test Library from CSV
        try {
            val categoryMaps = com.example.alearning.util.CsvParser.parse(context.assets.open("test_categories.csv"))
            val testMaps = com.example.alearning.util.CsvParser.parse(context.assets.open("tests.csv"))
            val normMaps = com.example.alearning.util.CsvParser.parse(context.assets.open("norms.csv"))

            android.util.Log.d("SeedDataManager", "Parsed ${categoryMaps.size} categories, ${testMaps.size} tests, ${normMaps.size} norms")

            val categories = categoryMaps.map { row ->
                TestCategoryEntity(
                    id = row["id"]!!,
                    name = row["name"]!!,
                    sortOrder = row["sortOrder"]?.toIntOrNull() ?: 0,
                    radarAxis = row["radarAxis"]
                )
            }

            val tests = testMaps.map { row ->
                val rawYoutubeId = row["youtube_id"]?.trim()
                val validatedYoutubeId = if (!rawYoutubeId.isNullOrEmpty()) {
                    if (rawYoutubeId.length == 11 && rawYoutubeId.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
                        rawYoutubeId
                    } else {
                        android.util.Log.w("SeedDataManager", "Invalid youtube_id '${rawYoutubeId}' for test ${row["id"]}. Must be exactly 11 characters. Nullifying.")
                        null
                    }
                } else null

                FitnessTestEntity(
                    id = row["id"]!!,
                    categoryId = row["categoryId"]!!,
                    name = row["name"]!!,
                    unit = row["unit"]!!,
                    isHigherBetter = row["isHigherBetter"]?.lowercase() == "true",
                    description = row["description"],
                    timingMode = row["timingMode"] ?: "MANUAL_ENTRY",
                    inputParadigm = row["inputParadigm"] ?: "NUMERIC",
                    athletesPerHeat = row["athletesPerHeat"]?.toIntOrNull(),
                    trialsPerAthlete = row["trialsPerAthlete"]?.toIntOrNull() ?: 1,
                    validMin = row["validMin"]?.toDoubleOrNull(),
                    validMax = row["validMax"]?.toDoubleOrNull(),
                    interpretationStrategy = row["interpretationStrategy"] ?: "NORM_LOOKUP",
                    calculationConfig = row["calculationConfig"],
                    youtubeId = validatedYoutubeId
                )
            }

            val norms = normMaps.map { row ->
                NormReferenceEntity(
                    testId = row["testId"]!!,
                    variant = row["variant"] ?: "Default",
                    sex = BiologicalSex.valueOf(row["sex"]?.uppercase() ?: "MALE"),
                    ageMin = row["ageMin"]?.toFloatOrNull() ?: 0f,
                    ageMax = row["ageMax"]?.toFloatOrNull() ?: 99f,
                    minScore = row["minScore"]?.toDoubleOrNull() ?: 0.0,
                    maxScore = row["maxScore"]?.toDoubleOrNull() ?: 999.0,
                    percentile = row["percentile"]?.toIntOrNull() ?: 0,
                    classification = row["classification"]
                )
            }

            importStandardsUseCase(
                categories.map { it.toDomain() },
                tests.map { it.toDomain() },
                norms.map { it.toDomain() },
                clearExisting = true
            )
            android.util.Log.d("SeedDataManager", "Successfully imported standards into database")

            // --- Seed Dummy Athletes & Results ---
            val cursor = database.query("SELECT COUNT(*) FROM individuals", null)
            var count = 0
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
            if (count == 0) {
                android.util.Log.d("SeedDataManager", "Seeding dummy athletes, groups, events, and results...")
                val peopleDao = database.peopleDao()
                val testingDao = database.testingDao()

                // 1. Groups
                val varsityGroup = GroupEntity(
                    id = "group_varsity_id",
                    name = "Varsity Football",
                    location = "Main Field",
                    cycle = "Fall 2026",
                    category = "TEAM"
                )
                val juniorGroup = GroupEntity(
                    id = "group_junior_id",
                    name = "Junior Basketball",
                    location = "Gymnasium",
                    cycle = "Winter 2026",
                    category = "TEAM"
                )
                peopleDao.insertGroup(varsityGroup)
                peopleDao.insertGroup(juniorGroup)

                // 2. Individuals (Athletes)
                val athletes = listOf(
                    IndividualEntity(
                        id = "athlete_alex",
                        firstName = "Alex",
                        lastName = "Mercer",
                        dateOfBirth = Calendar.getInstance().apply { set(2008, 0, 1) }.timeInMillis,
                        sex = BiologicalSex.MALE,
                        medicalAlert = null,
                        isRestricted = false
                    ),
                    IndividualEntity(
                        id = "athlete_sarah",
                        firstName = "Sarah",
                        lastName = "Connor",
                        dateOfBirth = Calendar.getInstance().apply { set(2009, 5, 12) }.timeInMillis,
                        sex = BiologicalSex.FEMALE,
                        medicalAlert = "Asthma",
                        isRestricted = false
                    ),
                    IndividualEntity(
                        id = "athlete_marcus",
                        firstName = "Marcus",
                        lastName = "Fenix",
                        dateOfBirth = Calendar.getInstance().apply { set(2007, 10, 20) }.timeInMillis,
                        sex = BiologicalSex.MALE,
                        medicalAlert = "Previous ACL Sprain",
                        isRestricted = false
                    ),
                    IndividualEntity(
                        id = "athlete_lara",
                        firstName = "Lara",
                        lastName = "Croft",
                        dateOfBirth = Calendar.getInstance().apply { set(2009, 2, 14) }.timeInMillis,
                        sex = BiologicalSex.FEMALE,
                        medicalAlert = null,
                        isRestricted = false
                    ),
                    IndividualEntity(
                        id = "athlete_john",
                        firstName = "John",
                        lastName = "Doe",
                        dateOfBirth = Calendar.getInstance().apply { set(2008, 11, 25) }.timeInMillis,
                        sex = BiologicalSex.MALE,
                        medicalAlert = null,
                        isRestricted = false
                    )
                )
                for (athlete in athletes) {
                    peopleDao.insertIndividual(athlete)
                }

                // 3. Group Memberships
                peopleDao.addMemberToGroup(GroupMemberCrossRef("group_varsity_id", "athlete_alex"))
                peopleDao.addMemberToGroup(GroupMemberCrossRef("group_varsity_id", "athlete_sarah"))
                peopleDao.addMemberToGroup(GroupMemberCrossRef("group_varsity_id", "athlete_marcus"))
                peopleDao.addMemberToGroup(GroupMemberCrossRef("group_varsity_id", "athlete_lara"))

                peopleDao.addMemberToGroup(GroupMemberCrossRef("group_junior_id", "athlete_lara"))
                peopleDao.addMemberToGroup(GroupMemberCrossRef("group_junior_id", "athlete_john"))

                // 4. Events
                val event1 = TestingEventEntity(
                    id = "event_benchmark_1",
                    groupId = "group_varsity_id",
                    name = "Summer Benchmark Testing",
                    date = System.currentTimeMillis() - 86400000L * 10L, // 10 days ago
                    location = "Main Field"
                )
                val event2 = TestingEventEntity(
                    id = "event_benchmark_2",
                    groupId = "group_varsity_id",
                    name = "Winter Combine Trials",
                    date = System.currentTimeMillis() - 86400000L * 30L, // 30 days ago
                    location = "Main Gym"
                )
                testingDao.insertEvent(event1)
                testingDao.insertEvent(event2)

                // 5. Link tests to events
                val testIds = listOf(
                    "test_pullup",
                    "test_mile_run",
                    "test_pacer",
                    "test_1rm_squat",
                    "test_pro_agility"
                )
                for (testId in testIds) {
                    testingDao.addTestToEvent(EventTestCrossRef("event_benchmark_1", testId))
                    testingDao.addTestToEvent(EventTestCrossRef("event_benchmark_2", testId))
                }

                // 6. Test Results
                val results = listOf(
                    // Alex Mercer
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_alex", testId = "test_pullup", rawScore = 15.0, ageAtTime = 18f, percentile = 85, classification = "SUPERIOR"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_alex", testId = "test_mile_run", rawScore = 320.0, ageAtTime = 18f, percentile = 90, classification = "SUPERIOR"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_alex", testId = "test_pacer", rawScore = 95.0, ageAtTime = 18f, percentile = 75, classification = "HEALTHY"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_alex", testId = "test_1rm_squat", rawScore = 315.0, ageAtTime = 18f, percentile = 80, classification = "HEALTHY"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_alex", testId = "test_pro_agility", rawScore = 4.25, ageAtTime = 18f, percentile = 88, classification = "SUPERIOR"),

                    // Sarah Connor
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_sarah", testId = "test_pullup", rawScore = 8.0, ageAtTime = 17f, percentile = 78, classification = "HEALTHY"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_sarah", testId = "test_mile_run", rawScore = 420.0, ageAtTime = 17f, percentile = 65, classification = "HEALTHY"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_sarah", testId = "test_pacer", rawScore = 70.0, ageAtTime = 17f, percentile = 82, classification = "SUPERIOR"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_sarah", testId = "test_1rm_squat", rawScore = 185.0, ageAtTime = 17f, percentile = 72, classification = "HEALTHY"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_sarah", testId = "test_pro_agility", rawScore = 4.60, ageAtTime = 17f, percentile = 74, classification = "HEALTHY"),

                    // Marcus Fenix
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_marcus", testId = "test_pullup", rawScore = 4.0, ageAtTime = 19f, percentile = 25, classification = "NEEDS_IMPROVEMENT"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_marcus", testId = "test_mile_run", rawScore = 540.0, ageAtTime = 19f, percentile = 20, classification = "NEEDS_IMPROVEMENT"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_marcus", testId = "test_pacer", rawScore = 35.0, ageAtTime = 19f, percentile = 28, classification = "NEEDS_IMPROVEMENT"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_marcus", testId = "test_1rm_squat", rawScore = 405.0, ageAtTime = 19f, percentile = 95, classification = "SUPERIOR"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_marcus", testId = "test_pro_agility", rawScore = 5.15, ageAtTime = 19f, percentile = 22, classification = "NEEDS_IMPROVEMENT"),

                    // Lara Croft
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_lara", testId = "test_pullup", rawScore = 12.0, ageAtTime = 17f, percentile = 94, classification = "SUPERIOR"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_lara", testId = "test_mile_run", rawScore = 390.0, ageAtTime = 17f, percentile = 92, classification = "SUPERIOR"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_lara", testId = "test_pacer", rawScore = 85.0, ageAtTime = 17f, percentile = 96, classification = "SUPERIOR"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_lara", testId = "test_1rm_squat", rawScore = 225.0, ageAtTime = 17f, percentile = 90, classification = "SUPERIOR"),
                    TestResultEntity(eventId = "event_benchmark_1", individualId = "athlete_lara", testId = "test_pro_agility", rawScore = 4.12, ageAtTime = 17f, percentile = 95, classification = "SUPERIOR")
                )
                for (res in results) {
                    testingDao.insertResult(res)
                }
                android.util.Log.d("SeedDataManager", "Successfully seeded dummy athletes, groups, events, and results!")
            }
        } catch (e: Exception) {
            android.util.Log.e("SeedDataManager", "Error seeding from CSV", e)
            e.printStackTrace()
        }

        prefs.edit().putBoolean(KEY_DATA_SEEDED, true).apply()
        android.util.Log.d("SeedDataManager", "Seeding complete")
    }
}
