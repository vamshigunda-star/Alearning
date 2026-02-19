package com.example.alearning.domain.usecase.standards

import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.standards.TestCategory
import com.example.alearning.domain.repository.StandardsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTestLibraryUseCase @Inject constructor(
    private val repository: StandardsRepository
) {
    fun getCategories(): Flow<List<TestCategory>> {
        return repository.getAllCategories()
    }

    fun getTestsByCategory(categoryId: String): Flow<List<FitnessTest>> {
        return repository.getTestsByCategory(categoryId)
    }
}
