package com.example.alearning.ui.analytics.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.alearning.ui.analytics.AnalyticsUiState
import com.example.alearning.ui.analytics.AnalyticsViewModel
import com.example.alearning.ui.theme.NavyPrimary
import com.example.alearning.ui.theme.PerformanceGreenText
import com.example.alearning.ui.theme.PerformanceRed
import com.example.alearning.ui.theme.PerformanceRedBorder
import com.example.alearning.ui.theme.PerformanceRedText
import com.example.alearning.ui.theme.PerformanceYellow
import com.example.alearning.ui.theme.PerformanceYellowText
import com.example.alearning.ui.theme.SportOrange

@Composable
fun RemediationTab(state: AnalyticsUiState, navController: NavController?, viewModel: AnalyticsViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ThresholdCard(state, viewModel) }

        if (state.domainPatterns.isNotEmpty()) {
            item { SectionHeader("Falling Domain Patterns", "Concentrations of weakness across the roster") }
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.domainPatterns) { pattern ->
                        DomainPatternCard(
                            domainName = pattern.domainName,
                            count = pattern.atRiskCount,
                            isCluster = pattern.isClusterPattern
                        )
                    }
                }
            }
        }

        item { SectionHeader("At-Risk Athletes", "Sorted by lowest average score") }

        if (state.atRiskAthletes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.GroupWork,
                                contentDescription = null,
                                tint = PerformanceGreenText,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No athletes at risk under this threshold.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        } else {
            items(state.atRiskAthletes) { athlete ->
                AtRiskAthleteCard(
                    name = athlete.athleteName,
                    group = athlete.groupName,
                    weakDomains = athlete.weakDomains,
                    avgScore = athlete.avgScore,
                    onClick = { navController?.navigate("athlete/${athlete.athleteId}") }
                )
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun ThresholdCard(state: AnalyticsUiState, viewModel: AnalyticsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SportOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = SportOrange, modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("At-Risk Threshold", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                    Text(
                        "Athletes scoring below this average are flagged.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NavyPrimary)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${(state.riskThreshold * 100).toInt()}%",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Slider(
                value = state.riskThreshold,
                onValueChange = { viewModel.updateThreshold(it) },
                valueRange = 0.2f..0.6f,
                steps = 7,
                colors = SliderDefaults.colors(
                    thumbColor = SportOrange,
                    activeTrackColor = SportOrange,
                    inactiveTrackColor = Color(0xFFECEFF1)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = PerformanceRedText, modifier = Modifier.size(16.dp))
                Text(
                    "${state.remediationCount} athletes at risk across ${state.domainPatterns.size} domain${if (state.domainPatterns.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NavyPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DomainPatternCard(domainName: String, count: Int, isCluster: Boolean) {
    val bg = if (isCluster) PerformanceRed else PerformanceYellow
    val fg = if (isCluster) PerformanceRedText else PerformanceYellowText
    Card(
        modifier = Modifier.width(180.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                domainName.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = fg
            )
            Text("$count athletes", style = MaterialTheme.typography.bodySmall, color = fg)
            if (isCluster) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(fg)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("CLUSTER", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AtRiskAthleteCard(
    name: String,
    group: String,
    weakDomains: List<String>,
    avgScore: Float,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PerformanceRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = PerformanceRedText, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                Text(group, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                if (weakDomains.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        weakDomains.take(3).forEach { domain ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PerformanceRed)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    domain.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PerformanceRedText
                                )
                            }
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${(avgScore * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PerformanceRedText
                )
                Text("avg", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
        // Bottom border accent
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(PerformanceRedBorder)
        )
    }
}
