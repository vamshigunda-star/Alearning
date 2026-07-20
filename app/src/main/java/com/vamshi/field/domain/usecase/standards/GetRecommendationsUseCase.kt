package com.vamshi.field.domain.usecase.standards

import com.vamshi.field.domain.model.standards.FitnessTest
import com.vamshi.field.domain.model.standards.RecommendationCategory
import com.vamshi.field.domain.repository.RecommendationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecommendationsUseCase @Inject constructor(
    private val repository: RecommendationRepository
) {
    fun getCategories(): Flow<List<RecommendationCategory>> {
        return repository.getRecommendationCategories()
    }

    fun getRecommendedTests(categoryId: String): Flow<List<FitnessTest>> {
        return repository.getRecommendedTests(categoryId)
    }
}
