package com.vamshi.field.domain.model.analytics

sealed interface AnalyticsAction {
    data class SelectIndividual(val individualId: String?) : AnalyticsAction
    data object GenerateAIPrescription : AnalyticsAction
}
