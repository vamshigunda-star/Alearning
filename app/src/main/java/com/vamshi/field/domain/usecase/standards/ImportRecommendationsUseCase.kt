package com.vamshi.field.domain.usecase.standards

import com.vamshi.field.domain.model.standards.RecommendationCategory
import com.vamshi.field.domain.model.standards.RecommendationTestLink
import com.vamshi.field.domain.repository.RecommendationRepository
import javax.inject.Inject

class ImportRecommendationsUseCase @Inject constructor(
    private val repository: RecommendationRepository
) {
    suspend operator fun invoke(
        categories: List<RecommendationCategory>,
        links: List<RecommendationTestLink>,
        clearExisting: Boolean = false
    ) {
        if (clearExisting) {
            repository.clearAllRecommendations()
        }
        repository.importRecommendations(categories, links)
    }
}
