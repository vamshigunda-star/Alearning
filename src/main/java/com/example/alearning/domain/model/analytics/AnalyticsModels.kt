package com.example.alearning.domain.model.analytics

import java.time.LocalDate

enum class TimePeriod(val label: String, val months: Int) {
    LAST_3_MONTHS("3M", 3),
    LAST_6_MONTHS("6M", 6),
    LAST_12_MONTHS("12M", 12),
    ALL("All", 0)
}

data class GroupProgressPoint(
    val date: LocalDate,
    val avgScore: Float,
)

data class AtRiskAthlete(
    val athleteId: String,
    val athleteName: String,
    val groupName: String,
    val weakDomains: List<String>,
    val avgScore: Float,
    val lastTestDate: LocalDate?,
)

data class DomainPattern(
    val domainName: String,
    val atRiskCount: Int,
    val avgScore: Float,
    val isClusterPattern: Boolean,
)

enum class InsightSeverity { CRITICAL, WARNING, POSITIVE }

data class PriorityInsight(
    val title: String,
    val description: String,
    val severity: InsightSeverity,
    val relatedDomain: String?,
    val affectedCount: Int,
)

enum class MoveDirection { IMPROVED, DECLINED }

data class AthleteMover(
    val athleteId: String,
    val athleteName: String,
    val direction: MoveDirection,
    val scoreDelta: Float,
    val domain: String,
)

data class RecommendedAction(
    val action: String,
    val rationale: String,
    val targetGroup: String?,
    val affectedAthletes: Int,
)
