package com.vamshi.field.domain.repository

import com.vamshi.field.domain.model.reports.AthleteDashboardData
import com.vamshi.field.domain.model.reports.AthleteTestDetailData
import com.vamshi.field.domain.model.reports.GroupOverviewData
import com.vamshi.field.domain.model.reports.ReportsHomeData
import com.vamshi.field.domain.model.reports.SessionReportData
import kotlinx.coroutines.flow.Flow

interface ReportsRepository {
    fun observeHome(): Flow<ReportsHomeData>
    fun observeGroupOverview(groupId: String): Flow<GroupOverviewData?>
    fun observeSessionReport(groupId: String, sessionId: String): Flow<SessionReportData?>
    fun observeAthleteDashboard(athleteId: String, contextSessionId: String?): Flow<AthleteDashboardData?>
    fun observeAthleteTestDetail(
        athleteId: String,
        testId: String,
        contextSessionId: String?
    ): Flow<AthleteTestDetailData?>
}
