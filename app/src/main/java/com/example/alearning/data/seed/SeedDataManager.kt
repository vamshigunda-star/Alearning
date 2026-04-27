package com.example.alearning.data.seed

import android.content.Context
import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.data.local.entities.standards.NormReferenceEntity
import com.example.alearning.data.local.entities.standards.TestCategoryEntity
import com.example.alearning.data.mapper.standards.toDomain
import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.usecase.people.RegisterAthleteUseCase
import com.example.alearning.domain.usecase.standards.ImportStandardsUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Calendar

@Singleton
class SeedDataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importStandardsUseCase: ImportStandardsUseCase,
    private val registerAthleteUseCase: RegisterAthleteUseCase
) {
    companion object {
        private const val PREFS_NAME = "alearning_prefs"
        private const val KEY_DATA_SEEDED = "data_seeded_csv_v6" // Bumped version for added norms
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
                    calculationConfig = row["calculationConfig"]
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
        } catch (e: Exception) {
            android.util.Log.e("SeedDataManager", "Error seeding from CSV", e)
            e.printStackTrace()
        }

        // 2. Seed Dummy Athletes
        seedAthletes()
        android.util.Log.d("SeedDataManager", "Successfully seeded athletes")

        prefs.edit().putBoolean(KEY_DATA_SEEDED, true).apply()
        android.util.Log.d("SeedDataManager", "Seeding complete")
    }

    private suspend fun seedAthletes() {
        val athletes = listOf(
            AthleteSeed("John", "Doe", 2010, BiologicalSex.MALE),
            AthleteSeed("Jane", "Smith", 2012, BiologicalSex.FEMALE),
            AthleteSeed("Sam", "Rivera", 2011, BiologicalSex.MALE)
        )

        athletes.forEach { 
            val cal = Calendar.getInstance()
            cal.set(it.birthYear, 0, 1)
            registerAthleteUseCase(
                firstName = it.firstName,
                lastName = it.lastName,
                dateOfBirth = cal.timeInMillis,
                sex = it.sex
            )
        }
    }

    private data class AthleteSeed(val firstName: String, val lastName: String, val birthYear: Int, val sex: BiologicalSex)
}
