package com.vamshi.field.domain.model.standards

/**
 * Transport type between the CSV seed pipeline and RecommendationRepository.importRecommendations.
 * Not exposed to the UI layer.
 */
data class RecommendationTestLink(
    val recommendationCategoryId: String,
    val testId: String,
    val sortOrder: Int = 0,
    val required: Boolean = true
)
