package com.example.alearning.data.local.daos.standards


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.data.local.entities.standards.NormReferenceEntity
import com.example.alearning.data.local.entities.standards.TestCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StandardsDao {

    // --- BROWSING TESTS ---

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: TestCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTest(test: FitnessTestEntity)

    // Flexible: Insert a whole list of norms at once (for Excel import)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNorms(norms: List<NormReferenceEntity>)
}