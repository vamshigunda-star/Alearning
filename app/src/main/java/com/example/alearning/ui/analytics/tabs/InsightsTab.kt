package com.example.alearning.ui.analytics.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alearning.domain.model.analytics.InsightSeverity
import com.example.alearning.domain.model.analytics.PriorityInsight
import com.example.alearning.domain.model.analytics.RecommendedAction
import com.example.alearning.ui.analytics.AnalyticsUiState
import com.example.alearning.ui.theme.NavyPrimary
import com.example.alearning.ui.theme.PerformanceGreen
import com.example.alearning.ui.theme.PerformanceGreenBorder
import com.example.alearning.ui.theme.PerformanceGreenText
import com.example.alearning.ui.theme.PerformanceRed
import com.example.alearning.ui.theme.PerformanceRedBorder
import com.example.alearning.ui.theme.PerformanceRedText
import com.example.alearning.ui.theme.PerformanceYellow
import com.example.alearning.ui.theme.PerformanceYellowBorder
import com.example.alearning.ui.theme.PerformanceYellowText
import com.example.alearning.ui.theme.SportOrange

private data class InsightStyle(
    val bg: Color,
    val border: Color,
    val fg: Color,
    val icon: ImageVector,
    val tag: String
)

private fun styleFor(severity: InsightSeverity): InsightStyle = when (severity) {
    InsightSeverity.CRITICAL -> InsightStyle(PerformanceRed, PerformanceRedBorder, PerformanceRedText, Icons.Default.Warning, "CRITICAL")
    InsightSeverity.WARNING -> InsightStyle(PerformanceYellow, PerformanceYellowBorder, PerformanceYellowText, Icons.Default.Info, "WATCH")
    InsightSeverity.POSITIVE -> InsightStyle(PerformanceGreen, PerformanceGreenBorder, PerformanceGreenText, Icons.Default.CheckCircle, "STRENGTH")
}

@Composable
fun InsightsTab(state: AnalyticsUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.strengthsToLeverage.isNotEmpty()) {
            item { SectionHeader("Strengths to Leverage", "Domains the roster excels in") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.strengthsToLeverage.forEach { strength ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(PerformanceGreen)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = PerformanceGreenText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    strength.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                                    color = PerformanceGreenText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        item { SectionHeader("Priority Insights", "Sorted by severity") }

        if (state.priorityInsights.isEmpty()) {
            item { EmptyCard("No insights yet — record more data to surface trends.") }
        } else {
            items(state.priorityInsights.take(6).size) { idx ->
                PriorityInsightCard(state.priorityInsights[idx])
            }
        }

        item { SectionHeader("Recommended Actions", "Suggested next steps for the coach") }

        if (state.recommendedActions.isEmpty()) {
            item { EmptyCard("No recommendations at the moment.") }
        } else {
            items(state.recommendedActions.size) { idx ->
                RecommendedActionCard(index = idx + 1, action = state.recommendedActions[idx])
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun PriorityInsightCard(insight: PriorityInsight) {
    val style = styleFor(insight.severity)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = style.bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(style.fg.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(style.icon, contentDescription = null, tint = style.fg, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(insight.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = style.fg)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(style.fg)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(style.tag, color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(insight.description, style = MaterialTheme.typography.bodyMedium, color = NavyPrimary)
                    insight.relatedDomain?.let { domain ->
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.6f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                domain.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                color = style.fg,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(style.border))
        }
    }
}

@Composable
private fun RecommendedActionCard(index: Int, action: RecommendedAction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(SportOrange),
                contentAlignment = Alignment.Center
            ) {
                Text("$index", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(action.action, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                Text(action.rationale, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                if (action.targetGroup != null) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = SportOrange, modifier = Modifier.size(14.dp))
                        Text(
                            "Target: ${action.targetGroup}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SportOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
            EmptyHint(message)
        }
    }
}
