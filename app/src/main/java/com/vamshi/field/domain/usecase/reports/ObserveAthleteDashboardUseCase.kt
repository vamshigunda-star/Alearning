package com.example.alearning.domain.usecase.reports

import com.example.alearning.domain.model.reports.AthleteDashboardData
import com.example.alearning.domain.repository.ReportsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAthleteDashboardUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository
) {
    operator fun invoke(athleteId: String, contextSessionId: String?): Flow<AthleteDashboardData?> {
        return reportsRepository.observeAthleteDashboard(athleteId, contextSessionId)
    }
}
