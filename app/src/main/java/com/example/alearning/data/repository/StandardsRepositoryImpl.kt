package com.example.alearning.data.repository

import com.example.alearning.data.local.daos.standards.StandardsDao
import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.data.local.entities.standards.NormReferenceEntity
import com.example.alearning.data.local.entities.standards.TestCategoryEntity
import com.example.alearning.domain.repository.StandardsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class StandardsRepositoryImpl @Inject constructor(
    private val dao: StandardsDao
) : StandardsRepository {

    // --- BROWSING ---
    override fun getAllCategories(): Flow<List<TestCategoryEntity>> {
        return dao.getAllCategories()
    }

    override fun getTestsByCategory(categoryId: String): Flow<List<FitnessTestEntity>> {
        return dao.getTestsByCategory(categoryId)
    }

    override suspend fun getTestById(testId: String): FitnessTestEntity? {
        return dao.getTestById(testId)
    }

    // --- INTERPRETATION ---
    override suspend fun getNormResult(
        testId: String,
        sex: String,
        age: Double,
        score: Double
    ): NormReferenceEntity? {
        // Connects to the specific query in your StandardsDao
        return dao.findNormResult(testId, sex, age, score)
    }

    // --- SETUP / IMPORT ---
    override suspend fun importStandards(
        categories: List<TestCategoryEntity>,
        tests: List<FitnessTestEntity>,
        norms: List<NormReferenceEntity>
    ) {
        // Executes bulk inserts
        categories.forEach { dao.insertCategory(it) }
        tests.forEach { dao.insertTest(it) }
        dao.insertNorms(norms)
    }
}