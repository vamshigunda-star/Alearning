package com.example.alearning.domain.repository

import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.data.local.entities.standards.NormReferenceEntity
import com.example.alearning.data.local.entities.standards.TestCategoryEntity
import kotlinx.coroutines.flow.Flow

interface StandardsRepository {

    // --- Browsing ---
    fun getAllCategories(): Flow<List<TestCategoryEntity>>

    fun getTestsByCategory(categoryId: String): Flow<List<FitnessTestEntity>>

    suspend fun getTestById(testId: String): FitnessTestEntity?

    // --- The "Magic" Lookup ---
    suspend fun getNormResult(
        testId: String,
        sex: String,
        age: Double,
        score: Double
    ): NormReferenceEntity?

    // --- Admin / Setup ---
    suspend fun importStandards(
        categories: List<TestCategoryEntity>,
        tests: List<FitnessTestEntity>,
        norms: List<NormReferenceEntity>
    )
}