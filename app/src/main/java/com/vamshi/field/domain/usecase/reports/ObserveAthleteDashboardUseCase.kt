package com.vamshi.field.domain.usecase.reports

import com.vamshi.field.domain.model.reports.AthleteDashboardData
import com.vamshi.field.domain.repository.ReportsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAthleteDashboardUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository
) {
    operator fun invoke(athleteId: String, contextSessionId: String?): Flow<AthleteDashboardData?> {
        return reportsRepository.observeAthleteDashboard(athleteId, contextSessionId)
    }
}
