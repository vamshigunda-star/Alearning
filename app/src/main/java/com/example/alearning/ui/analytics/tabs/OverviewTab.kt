package com.example.alearning.ui.analytics.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alearning.ui.analytics.AnalyticsUiState
import com.example.alearning.ui.components.charts.RadarChart
import com.example.alearning.ui.theme.NavyPrimary
import com.example.alearning.ui.theme.PerformanceGreen
import com.example.alearning.ui.theme.PerformanceGreenText
import com.example.alearning.ui.theme.PerformanceRed
import com.example.alearning.ui.theme.PerformanceRedText
import com.example.alearning.ui.theme.PerformanceYellowText
import com.example.alearning.ui.theme.SportOrange

@Composable
fun OverviewTab(state: AnalyticsUiState) {
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = NavyPrimary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { HeroStatsRow(state) }

        item { SectionHeader("Roster Fitness Profile", "Average performance across active athletes") }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.rosterRadarData != null && state.rosterRadarData.axisScores.isNotEmpty()) {
                        RadarChart(
                            data = state.rosterRadarData,
                            modifier = Modifier.height(300.dp),
                            color = SportOrange
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                            EmptyHint("No radar data yet — record some test results to see this chart.")
                        }
                    }
                }
            }
        }

        item { SectionHeader("Domain Summary", "Strongest and weakest fitness areas") }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DomainCard(
                    label = "Strongest",
                    domain = state.strongestDomain,
                    bg = PerformanceGreen,
                    fg = PerformanceGreenText,
                    icon = Icons.Default.ArrowUpward,
                    modifier = Modifier.weight(1f)
                )
                DomainCard(
                    label = "Weakest",
                    domain = state.weakestDomain,
                    bg = PerformanceRed,
                    fg = PerformanceRedText,
                    icon = Icons.Default.ArrowDownward,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item { SectionHeader("Score Distribution", "How the roster sits across performance zones") }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    if (state.scoreDistribution.isEmpty()) {
                        EmptyHint("No scores recorded yet.")
                    } else {
                        val total = state.scoreDistribution.values.sum().coerceAtLeast(1)
                        state.scoreDistribution.entries
                            .sortedByDescending { it.value }
                            .forEachIndexed { idx, (classification, count) ->
                                if (idx > 0) Spacer(Modifier.height(14.dp))
                                DistributionRow(
                                    label = classification,
                                    count = count,
                                    fraction = count.toFloat() / total
                                )
                            }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun HeroStatsRow(state: AnalyticsUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatTile(
            value = state.totalAthletes.toString(),
            label = "Athletes",
            icon = Icons.Default.Group,
            accent = SportOrange,
            modifier = Modifier.weight(1f)
        )
        StatTile(
            value = "${(state.dataCompleteness * 100).toInt()}%",
            label = "Coverage",
            icon = Icons.Default.CheckCircle,
            accent = PerformanceGreenText,
            modifier = Modifier.weight(1f)
        )
        val avg = state.rosterRadarData?.axisScores
            ?.takeIf { it.isNotEmpty() }
            ?.map { it.normalizedScore }
            ?.average()
            ?.let { (it * 100).toInt() }
        StatTile(
            value = avg?.let { "$it%" } ?: "—",
            label = "Avg Score",
            icon = Icons.Default.Insights,
            accent = NavyPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NavyPrimary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
private fun DomainCard(
    label: String,
    domain: String?,
    bg: Color,
    fg: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = fg, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                domain?.replace('_', ' ')?.lowercase()?.replaceFirstChar { it.uppercase() } ?: "—",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = fg
            )
        }
    }
}

@Composable
private fun DistributionRow(label: String, count: Int, fraction: Float) {
    val (bar, text) = when {
        label.contains("Superior", true) || label.contains("Excellent", true) ->
            PerformanceGreenText to PerformanceGreenText
        label.contains("Healthy", true) || label.contains("Good", true) ->
            NavyPrimary to NavyPrimary
        label.contains("Need", true) || label.contains("Poor", true) ->
            PerformanceRedText to PerformanceRedText
        label.contains("Caution", true) || label.contains("Borderline", true) ->
            PerformanceYellowText to PerformanceYellowText
        else -> NavyPrimary to NavyPrimary
    }
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = NavyPrimary)
            Text("$count · ${(fraction * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = text, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFFECEFF1))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(999.dp))
                    .background(bar)
            )
        }
    }
}

@Composable
internal fun SectionHeader(title: String, subtitle: String? = null) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
internal fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
}
