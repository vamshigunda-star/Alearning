package com.vamshi.field.domain.repository

import com.vamshi.field.domain.model.people.BiologicalSex
import com.vamshi.field.domain.model.standards.FitnessTest
import com.vamshi.field.domain.model.standards.NormReference
import com.vamshi.field.domain.model.standards.TestCategory
import kotlinx.coroutines.flow.Flow

interface StandardsRepository {

    // --- Browsing ---
    fun getAllCategories(): Flow<List<TestCategory>>

    fun getTestsByCategory(categoryId: String): Flow<List<FitnessTest>>

    fun getAllTests(): Flow<List<FitnessTest>>

    suspend fun getTestById(testId: String): FitnessTest?

    // --- The "Magic" Lookup ---
    suspend fun getNormResult(
        testId: String,
        sex: BiologicalSex,
        age: Double,
        score: Double
    ): NormReference?

    // --- Admin / Setup ---

    /** Upserts catalog rows in place; never deletes tests or categories. */
    suspend fun importStandards(
        categories: List<TestCategory>,
        tests: List<FitnessTest>
    )

    /** Atomically swaps the full norm table for the given set. */
    suspend fun replaceAllNorms(norms: List<NormReference>)
}
