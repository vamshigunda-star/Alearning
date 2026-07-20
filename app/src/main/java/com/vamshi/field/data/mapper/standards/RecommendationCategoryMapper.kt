package com.vamshi.field.data.mapper.standards

import com.vamshi.field.data.local.entities.standards.RecommendationCategoryEntity
import com.vamshi.field.data.local.entities.standards.RecommendationTestCrossRef
import com.vamshi.field.domain.model.standards.RecommendationCategory
import com.vamshi.field.domain.model.standards.RecommendationScope
import com.vamshi.field.domain.model.standards.RecommendationTestLink

fun RecommendationCategoryEntity.toDomain(): RecommendationCategory {
    return RecommendationCategory(
        id = this.id,
        name = this.name,
        description = this.description,
        icon = this.icon,
        scope = try { RecommendationScope.valueOf(this.scope) } catch (_: Exception) { RecommendationScope.POPULATION },
        sortOrder = this.sortOrder
    )
}

fun RecommendationCategory.toEntity(): RecommendationCategoryEntity {
    return RecommendationCategoryEntity(
        id = this.id,
        name = this.name,
        description = this.description,
        icon = this.icon,
        scope = this.scope.name,
        sortOrder = this.sortOrder
    )
}

fun RecommendationTestLink.toEntity(): RecommendationTestCrossRef {
    return RecommendationTestCrossRef(
        recommendationCategoryId = this.recommendationCategoryId,
        testId = this.testId,
        sortOrder = this.sortOrder,
        required = this.required
    )
}
