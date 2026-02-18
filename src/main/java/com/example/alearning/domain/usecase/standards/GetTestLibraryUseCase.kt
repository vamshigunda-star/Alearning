package com.example.alearning.domain.usecase.standards

import com.example.alearning.data.local.entities.standards.FitnessTestEntity
import com.example.alearning.data.local.entities.standards.TestCategoryEntity
import com.example.alearning.domain.repository.StandardsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTestLibraryUseCase @Inject constructor(
    private val repository: StandardsRepository
) {
    fun getCategories(): Flow<List<TestCategoryEntity>> {
        return repository.getAllCategories()
    }

    fun getTestsByCategory(categoryId: String): Flow<List<FitnessTestEntity>> {
        return repository.getTestsByCategory(categoryId)
    }
}
