package com.example.alearning.domain.usecase.reports

import com.example.alearning.domain.model.reports.AthleteTestDetailData
import com.example.alearning.domain.repository.ReportsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAthleteTestDetailUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository
) {
    operator fun invoke(athleteId: String, testId: String, contextSessionId: String?): Flow<AthleteTestDetailData?> {
        return reportsRepository.observeAthleteTestDetail(athleteId, testId, contextSessionId)
    }
}
