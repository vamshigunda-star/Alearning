package com.vamshi.field.domain.repository

import com.vamshi.field.domain.model.standards.FitnessTest
import com.vamshi.field.domain.model.standards.RecommendationCategory
import com.vamshi.field.domain.model.standards.RecommendationTestLink
import kotlinx.coroutines.flow.Flow

interface RecommendationRepository {

    fun getRecommendationCategories(): Flow<List<RecommendationCategory>>

    fun getRecommendedTests(categoryId: String): Flow<List<FitnessTest>>

    // --- Admin / Setup ---
    suspend fun importRecommendations(
        categories: List<RecommendationCategory>,
        links: List<RecommendationTestLink>
    )

    suspend fun clearAllRecommendations()
}
