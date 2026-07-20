package com.vamshi.field.domain.usecase.reports

import com.vamshi.field.domain.model.reports.AthleteTestDetailData
import com.vamshi.field.domain.repository.ReportsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAthleteTestDetailUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository
) {
    operator fun invoke(athleteId: String, testId: String, contextSessionId: String?): Flow<AthleteTestDetailData?> {
        return reportsRepository.observeAthleteTestDetail(athleteId, testId, contextSessionId)
    }
}
