package com.example.alearning.domain.repository

import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.standards.NormReference
import com.example.alearning.domain.model.standards.TestCategory
import kotlinx.coroutines.flow.Flow

interface StandardsRepository {

    // --- Browsing ---
    fun getAllCategories(): Flow<List<TestCategory>>

    fun getTestsByCategory(categoryId: String): Flow<List<FitnessTest>>

    suspend fun getTestById(testId: String): FitnessTest?

    // --- The "Magic" Lookup ---
    suspend fun getNormResult(
        testId: String,
        sex: BiologicalSex,
        age: Double,
        score: Double
    ): NormReference?

    // --- Admin / Setup ---
    suspend fun importStandards(
        categories: List<TestCategory>,
        tests: List<FitnessTest>
    )

    suspend fun insertNorms(norms: List<NormReference>)
}
