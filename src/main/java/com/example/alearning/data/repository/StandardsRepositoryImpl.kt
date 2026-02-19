package com.example.alearning.data.repository

import com.example.alearning.data.local.daos.standards.StandardsDao
import com.example.alearning.data.mapper.standards.toDomain
import com.example.alearning.data.mapper.standards.toEntity
import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.standards.NormReference
import com.example.alearning.domain.model.standards.TestCategory
import com.example.alearning.domain.repository.StandardsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class StandardsRepositoryImpl @Inject constructor(
    private val dao: StandardsDao
) : StandardsRepository {

    // --- BROWSING ---
    override fun getAllCategories(): Flow<List<TestCategory>> {
        return dao.getAllCategories().map { list -> list.map { it.toDomain() } }
    }

    override fun getTestsByCategory(categoryId: String): Flow<List<FitnessTest>> {
        return dao.getTestsByCategory(categoryId).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTestById(testId: String): FitnessTest? {
        return dao.getTestById(testId)?.toDomain()
    }

    // --- INTERPRETATION ---
    override suspend fun getNormResult(
        testId: String,
        sex: BiologicalSex,
        age: Double,
        score: Double
    ): NormReference? {
        return dao.findNormResult(testId, sex.name, age, score)?.toDomain()
    }

    // --- SETUP / IMPORT ---
    override suspend fun importStandards(
        categories: List<TestCategory>,
        tests: List<FitnessTest>
    ) {
        categories.forEach { dao.insertCategory(it.toEntity()) }
        tests.forEach { dao.insertTest(it.toEntity()) }
    }

    override suspend fun insertNorms(norms: List<NormReference>) {
        dao.insertNorms(norms.map { it.toEntity() })
    }
}
