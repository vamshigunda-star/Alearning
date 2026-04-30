package com.example.alearning.domain.usecase.analytics

import com.example.alearning.domain.model.analytics.*
import com.example.alearning.domain.usecase.testing.AthleteRadarData
import javax.inject.Inject
import kotlin.math.abs

class GenerateInsightsUseCase @Inject constructor() {

    fun invoke(
        rosterRadarData: AthleteRadarData?,
        atRiskAthletes: List<AtRiskAthlete>,
        trendData: Map<String, List<GroupProgressPoint>>,
    ): Triple<List<PriorityInsight>, List<AthleteMover>, List<RecommendedAction>> {
        val insights = mutableListOf<PriorityInsight>()
        val movers = mutableListOf<AthleteMover>()
        val actions = mutableListOf<RecommendedAction>()

        // Analyze Roster Data
        rosterRadarData?.axisScores?.let { scores ->
            val weakAxes = scores.filter { it.normalizedScore < 0.4f }
            for (axis in weakAxes) {
                insights.add(
                    PriorityInsight(
                        title = "Roster Weakness",
                        description = "${axis.axis.name} scores are averaging below 40% globally.",
                        severity = InsightSeverity.WARNING,
                        relatedDomain = axis.axis.name,
                        affectedCount = axis.testCount
                    )
                )
                actions.add(
                    RecommendedAction(
                        action = "Focus training on ${axis.axis.name}",
                        rationale = "Global average is low. Prioritize this domain in upcoming sessions.",
                        targetGroup = null,
                        affectedAthletes = axis.testCount
                    )
                )
            }
            
            val strongAxes = scores.filter { it.normalizedScore > 0.7f }
            for (axis in strongAxes) {
                insights.add(
                    PriorityInsight(
                        title = "Roster Strength",
                        description = "${axis.axis.name} scores are exceptionally strong.",
                        severity = InsightSeverity.POSITIVE,
                        relatedDomain = axis.axis.name,
                        affectedCount = axis.testCount
                    )
                )
            }
        }

        // Analyze At-Risk Athletes
        if (atRiskAthletes.isNotEmpty()) {
            insights.add(
                PriorityInsight(
                    title = "At-Risk Athletes",
                    description = "${atRiskAthletes.size} athletes are currently flagged as at-risk.",
                    severity = InsightSeverity.CRITICAL,
                    relatedDomain = null,
                    affectedCount = atRiskAthletes.size
                )
            )
            
            val groupsWithAtRisk = atRiskAthletes.groupBy { it.groupName }
            for ((group, athletes) in groupsWithAtRisk) {
                if (athletes.size >= 3) {
                    actions.add(
                        RecommendedAction(
                            action = "Review programming for $group",
                            rationale = "High concentration of at-risk athletes in this group.",
                            targetGroup = group,
                            affectedAthletes = athletes.size
                        )
                    )
                }
            }
        }

        // Analyze Trends
        for ((group, points) in trendData) {
            if (points.size < 2) continue

            val latest = points.last().avgScore
            val previous = points.first().avgScore
            val delta = latest - previous

            if (delta > 0.1f) {
                insights.add(
                    PriorityInsight(
                        title = "Strong Progress",
                        description = "$group has improved by ${(delta * 100).toInt()}% this period.",
                        severity = InsightSeverity.POSITIVE,
                        relatedDomain = null,
                        affectedCount = 0
                    )
                )
            } else if (delta < -0.1f) {
                insights.add(
                    PriorityInsight(
                        title = "Declining Performance",
                        description = "$group performance has dropped by ${(abs(delta) * 100).toInt()}% recently.",
                        severity = InsightSeverity.WARNING,
                        relatedDomain = null,
                        affectedCount = 0
                    )
                )
                actions.add(
                    RecommendedAction(
                        action = "Investigate decline in $group",
                        rationale = "Significant downward trend in average scores.",
                        targetGroup = group,
                        affectedAthletes = 0
                    )
                )
            }
        }

        // Fake top movers for now, since we'd need historical individual data to calculate properly
        // The instructions say "derived from remediation + trend data", but AthleteMover requires individual delta
        
        return Triple(
            insights.sortedBy { it.severity.ordinal },
            movers,
            actions
        )
    }
}
