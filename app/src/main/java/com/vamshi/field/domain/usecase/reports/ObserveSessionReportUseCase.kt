package com.vamshi.field.domain.usecase.reports

import com.vamshi.field.domain.model.reports.SessionReportData
import com.vamshi.field.domain.repository.ReportsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveSessionReportUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository
) {
    operator fun invoke(groupId: String, sessionId: String): Flow<SessionReportData?> {
        return reportsRepository.observeSessionReport(groupId, sessionId)
    }
}
