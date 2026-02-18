package com.example.alearning.data.seed

import android.content.Context
import com.example.alearning.data.local.entities.people.BiologicalSex
import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.data.local.entities.standards.NormReferenceEntity
import com.example.alearning.data.local.entities.standards.TestCategoryEntity
import com.example.alearning.domain.usecase.standards.ImportStandardsUseCase
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SeedDataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val importStandardsUseCase: ImportStandardsUseCase
) {
    companion object {
        private const val PREFS_NAME = "alearning_prefs"
        private const val KEY_DATA_SEEDED = "data_seeded_v1"
    }

    suspend fun seedIfNeeded() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_DATA_SEEDED, false)) return

        val json = context.assets.open("seed_fitness_tests.json")
            .bufferedReader().use { it.readText() }

        val seedData = Gson().fromJson(json, SeedJson::class.java)

        val categories = seedData.categories.map { c ->
            TestCategoryEntity(
                id = c.id,
                name = c.name,
                sortOrder = c.sortOrder
            )
        }

        val tests = seedData.tests.map { t ->
            FitnessTestEntity(
                id = t.id,
                categoryId = t.categoryId,
                name = t.name,
                unit = t.unit,
                isHigherBetter = t.isHigherBetter,
                description = t.description
            )
        }

        val norms = seedData.norms.map { n ->
            NormReferenceEntity(
                testId = n.testId,
                sex = BiologicalSex.valueOf(n.sex),
                ageMin = n.ageMin,
                ageMax = n.ageMax,
                minScore = n.minScore,
                maxScore = n.maxScore,
                percentile = n.percentile,
                classification = n.classification
            )
        }

        importStandardsUseCase(categories, tests, norms)
        prefs.edit().putBoolean(KEY_DATA_SEEDED, true).apply()
    }
}

private data class SeedJson(
    val categories: List<SeedCategory>,
    val tests: List<SeedTest>,
    val norms: List<SeedNorm>
)

private data class SeedCategory(
    val id: String,
    val name: String,
    val sortOrder: Int
)

private data class SeedTest(
    val id: String,
    val categoryId: String,
    val name: String,
    val unit: String,
    val isHigherBetter: Boolean,
    val description: String?
)

private data class SeedNorm(
    val testId: String,
    val sex: String,
    val ageMin: Float,
    val ageMax: Float,
    val minScore: Double,
    val maxScore: Double,
    val percentile: Int,
    val classification: String?
)
