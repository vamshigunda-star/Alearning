package com.vamshi.field.domain.model.standards

enum class RecommendationScope {
    POPULATION, SPORT, ORG, CUSTOM, AI
}

data class RecommendationCategory(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val scope: RecommendationScope = RecommendationScope.POPULATION,
    val sortOrder: Int = 0
)
