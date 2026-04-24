package com.example.alearning.data.local.daos.standards


import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.data.local.entities.standards.NormReferenceEntity
import com.example.alearning.data.local.entities.standards.TestCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StandardsDao {

    // --- BROWSING TESTS ---
    @Query("SELECT * FROM fitness_tests ORDER BY name ASC")
    fun getAllTests(): Flow<List<FitnessTestEntity>>

    // Also useful as a suspend version for one-time fetches:
    @Query("SELECT * FROM fitness_tests ORDER BY name ASC")
    suspend fun getAllTestsOnce(): List<FitnessTestEntity>

    @Query("SELECT * FROM test_categories ORDER BY sortOrder ASC")
    fun getAllCategories(): Flow<List<TestCategoryEntity>>

    @Query("SELECT * FROM fitness_tests WHERE categoryId = :categoryId ORDER BY name ASC")
    fun getTestsByCategory(categoryId: String): Flow<List<FitnessTestEntity>>

    @Query("SELECT * FROM fitness_tests WHERE id = :testId LIMIT 1")
    suspend fun getTestById(testId: String): FitnessTestEntity?

    // --- THE MAGIC LOOKUP (Interpretation) ---

    /**
     * Finds the exact norm row matching the student's age, sex, and score.
     * This provides the percentile and classification (e.g., "Excellent").
     */
    @Query("""
        SELECT * FROM norm_references 
        WHERE testId = :testId 
        AND sex = :sex 
        AND :age BETWEEN ageMin AND ageMax 
        AND :score BETWEEN minScore AND maxScore
        LIMIT 1
    """)
    suspend fun findNormResult(
        testId: String,
        sex: String,
        age: Double, // Changed to Double to match entity type usually, or Float if you prefer
        score: Double
    ): NormReferenceEntity?

    // --- DATA IMPORT (Admin/Setup) ---

    @Upsert
    suspend fun insertCategory(category: TestCategoryEntity)

    @Upsert
    suspend fun insertTest(test: FitnessTestEntity)

    // Flexible: Insert a whole list of norms at once (for Excel import)
    @Upsert
    suspend fun insertNorms(norms: List<NormReferenceEntity>)

    @Query("DELETE FROM norm_references")
    suspend fun deleteAllNorms()

    @Query("DELETE FROM fitness_tests")
    suspend fun deleteAllTests()

    @Query("DELETE FROM test_categories")
    suspend fun deleteAllCategories()
}
