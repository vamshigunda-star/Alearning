package com.example.alearning.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.interpretation.AthleteFlag
import com.example.alearning.interpretation.FlagType
import com.example.alearning.reports.GroupCardData
import com.example.alearning.reports.RecentSessionRow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(
    onNavigateToGroup: (String) -> Unit,
    onNavigateToSession: (String, String) -> Unit,
    onNavigateToAthlete: (String) -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(title = {
                Column {
                    Text("Reports", fontWeight = FontWeight.Bold)
                    Text("Across all groups", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            })
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            ReportContent(
                onNavigateToGroupReport = { groupId, sessionId ->
                    if (sessionId != null) {
                        onNavigateToSession(groupId, sessionId)
                    } else {
                        onNavigateToGroup(groupId)
                    }
                },
                viewModel = viewModel,
                onNavigateToAthlete = onNavigateToAthlete
            )
        }
    }
}

@Composable
fun ReportContent(
    onNavigateToGroupReport: (String, String?) -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
    onNavigateToAthlete: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    when {
        uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        uiState.data == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No reports yet", color = Color.Gray)
        }
        else -> ReportBody(
            uiState = uiState,
            padding = PaddingValues(0.dp),
            onAction = { action ->
                when (action) {
                    is ReportAction.OnNavigateToGroup -> onNavigateToGroupReport(action.groupId, null)
                    is ReportAction.OnNavigateToSession -> onNavigateToGroupReport(action.groupId, action.sessionId)
                    is ReportAction.OnNavigateToAthlete -> onNavigateToAthlete(action.individualId)
                    else -> viewModel.onAction(action)
                }
            }
        )
    }
}

@Composable
private fun ReportBody(
    uiState: ReportUiState,
    padding: PaddingValues,
    onAction: (ReportAction) -> Unit
) {
    val data = uiState.data!!
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            HeroStats(
                healthy = data.totalHealthy,
                flagged = data.totalFlagged,
                sessionsThisMonth = data.sessionsThisMonth
            )
        }

        item {
            Text("Needs Attention", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (data.flags.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))) {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No athletes flagged. ", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1B5E20))
                    }
                }
            }
        } else {
            items(data.flags, key = { "${it.individualId}-${it.type}" }) { flag ->
                FlagRow(flag = flag, onClick = { onAction(ReportAction.OnNavigateToAthlete(flag.individualId, null)) })
            }
        }

        item {
            Text("Browse by Group", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (data.groups.isEmpty()) {
            item { Text("No groups yet.", color = Color.Gray, style = MaterialTheme.typography.bodyMedium) }
        } else {
            items(data.groups, key = { it.group.id }) { card ->
                GroupCard(card = card, onClick = { onAction(ReportAction.OnNavigateToGroup(card.group.id)) })
            }
        }

        item {
            Text("Recent Sessions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        if (data.recentSessions.isEmpty()) {
            item {
                Text(
                    "No sessions yet — start a testing session to populate reports.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(data.recentSessions, key = { it.event.id }) { row ->
                RecentSessionCard(row = row, onClick = {
                    row.groupId?.let { gid -> onAction(ReportAction.OnNavigateToSession(gid, row.event.id)) }
                })
            }
        }
    }
}

@Composable
private fun HeroStats(healthy: Int, flagged: Int, sessionsThisMonth: Int) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            HeroStat(label = "Healthy", value = healthy.toString(), color = Color(0xFFA5D6A7))
            HeroStat(label = "Flagged", value = flagged.toString(), color = Color(0xFFEF9A9A))
            HeroStat(label = "Sessions this month", value = sessionsThisMonth.toString(), color = Color.White)
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f))
    }
}

@Composable
private fun FlagRow(flag: AthleteFlag, onClick: () -> Unit) {
    val (chipBg, chipFg) = when (flag.type) {
        FlagType.BELOW_HEALTHY -> Color(0xFFFFEBEE) to Color(0xFFB71C1C)
        FlagType.REGRESSION -> Color(0xFFFFF3E0) to Color(0xFFE65100)
        FlagType.ABSENT -> Color(0xFFECEFF1) to Color(0xFF455A64)
        FlagType.MISSING_DATA -> Color(0xFFFFFDE7) to Color(0xFFF57F17)
    }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(32.dp).background(Color(0xFFECEFF1), CircleShape), contentAlignment = Alignment.Center) {
                Text(flag.athleteName.firstOrNull()?.uppercaseChar()?.toString() ?: "?", style = MaterialTheme.typography.labelMedium)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(flag.athleteName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(flag.groupName, style = MaterialTheme.typography.labelSmall, color = Color.Gray, maxLines = 1)
            }
            Box(modifier = Modifier.background(chipBg, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text(
                    text = when (flag.type) {
                        FlagType.BELOW_HEALTHY -> "Below Healthy"
                        FlagType.REGRESSION -> "Regression"
                        FlagType.ABSENT -> "Absent"
                        FlagType.MISSING_DATA -> "Missing data"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = chipFg,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFB0BEC5), modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun GroupCard(card: GroupCardData, onClick: () -> Unit) {
    val df = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(card.group.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${card.size} athlete${if (card.size == 1) "" else "s"}" +
                            (card.lastSessionDate?.let { " · last session ${df.format(Date(it))}" } ?: ""),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSessionCard(row: RecentSessionRow, onClick: () -> Unit) {
    val df = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(row.event.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    text = (row.groupName?.plus(" · ") ?: "") + df.format(Date(row.event.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${row.testCount} test${if (row.testCount == 1) "" else "s"} · ${row.athleteTestedCount} athletes tested",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFFB0BEC5))
        }
    }
}
