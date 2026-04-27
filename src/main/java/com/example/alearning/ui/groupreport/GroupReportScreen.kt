package com.example.alearning.ui.groupreport

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.usecase.testing.GroupLeaderboard
import com.example.alearning.domain.usecase.testing.LeaderboardEntry
import com.example.alearning.domain.usecase.testing.RemediationFlag
import com.example.alearning.domain.usecase.testing.RemediationList
import com.example.alearning.ui.components.charts.ProgressLineChart
import com.example.alearning.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun GroupReportScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAthleteReport: (String) -> Unit,
    onNavigateToLeaderboard: (String, String, String) -> Unit,
    viewModel: GroupReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    GroupReportContent(
        uiState = uiState,
        eventId = viewModel.eventId ?: "",
        groupId = viewModel.groupId,
        onAction = { action ->
            when (action) {
                is GroupReportAction.OnNavigateBack -> onNavigateBack()
                is GroupReportAction.OnNavigateToAthleteReport -> onNavigateToAthleteReport(action.individualId)
                is GroupReportAction.OnNavigateToLeaderboard -> onNavigateToLeaderboard(action.eventId, action.groupId, action.mode)
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupReportContent(
    uiState: GroupReportUiState,
    eventId: String,
    groupId: String,
    onAction: (GroupReportAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Report") },
                navigationIcon = {
                    IconButton(onClick = { onAction(GroupReportAction.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState()
            uiState.errorMessage != null && uiState.event == null -> ErrorState(
                message = uiState.errorMessage,
                onDismiss = { onAction(GroupReportAction.OnDismissError) }
            )
            else -> {
                GroupReportBody(
                    uiState = uiState,
                    eventId = eventId,
                    groupId = groupId,
                    onAction = onAction,
                    padding = padding
                )
            }
        }
    }
}

@Composable
private fun LoadingState(message: String = "Loading...") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@Composable
private fun GroupReportBody(
    uiState: GroupReportUiState,
    eventId: String,
    groupId: String,
    onAction: (GroupReportAction) -> Unit,
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Event summary card (if eventId is provided)
        uiState.event?.let { event ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            event.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                        Text(
                            dateFormat.format(Date(event.date)),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // Test selection tabs
        if (uiState.tests.isNotEmpty()) {
            item {
                Text(
                    "Leaderboard",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                ScrollableTabRow(
                    selectedTabIndex = uiState.tests.indexOfFirst { it.id == uiState.selectedTestId }.coerceAtLeast(0),
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = { tabPositions ->
                        val index = uiState.tests.indexOfFirst { it.id == uiState.selectedTestId }.coerceAtLeast(0)
                        if (index < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[index]),
                                color = NavyPrimary
                            )
                        }
                    }
                ) {
                    uiState.tests.forEach { test ->
                        Tab(
                            selected = test.id == uiState.selectedTestId,
                            onClick = { onAction(GroupReportAction.OnSelectTest(test.id)) },
                            text = { 
                                Text(
                                    test.name, 
                                    maxLines = 1,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (test.id == uiState.selectedTestId) Color.Black else Color.Gray
                                ) 
                            }
                        )
                    }
                }
            }

            // Leaderboard results
            if (uiState.isLeaderboardLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            } else {
                uiState.leaderboard?.let { leaderboard ->
                    if (leaderboard.entries.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No results for this test yet", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                            }
                        }
                    } else {
                        items(leaderboard.entries) { entry ->
                            LeaderboardEntryCard(
                                entry = entry,
                                onClick = { onAction(GroupReportAction.OnNavigateToAthleteReport(entry.individualId)) }
                            )
                        }
                    }
                }
            }

            // Group Progress section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Group Trend",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            if (uiState.isProgressLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            } else {
                uiState.selectedTestProgress?.let { progress ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Average Percentile Over Time",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                if (progress.dataPoints.size >= 2) {
                                    ProgressLineChart(
                                        dataPoints = progress.dataPoints.map { it.averagePercentile / 100f },
                                        modifier = Modifier.height(180.dp),
                                        color = NavyPrimary
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(100.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Need at least 2 testing events to show trend",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Remediation section
        uiState.remediationList?.let { remediation ->
            if (remediation.flags.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = PerformanceRedText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Needs Attention (below ${remediation.thresholdPercentile}th percentile)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PerformanceRedText
                        )
                    }
                }

                items(remediation.flags) { flag ->
                    RemediationFlagCard(
                        flag = flag,
                        onClick = { onAction(GroupReportAction.OnNavigateToAthleteReport(flag.individualId)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardEntryCard(
    entry: LeaderboardEntry,
    onClick: () -> Unit
) {
    val (bgColor, textColor) = when {
        entry.percentile == null -> PerformanceGrey to PerformanceGreyText
        entry.percentile >= 70 -> PerformanceGreen.copy(alpha = 0.7f) to PerformanceGreenText
        entry.percentile >= 35 -> PerformanceYellow.copy(alpha = 0.7f) to PerformanceYellowText
        else -> PerformanceRed.copy(alpha = 0.7f) to PerformanceRedText
    }

    val classification = when {
        entry.percentile == null -> "—"
        entry.percentile >= 70 -> "EXCELLENT"
        entry.percentile >= 35 -> "HEALTHY"
        else -> "NEEDS IMP."
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // RANK
            Text(
                text = entry.rank.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = NavyPrimary.copy(alpha = 0.2f),
                modifier = Modifier.width(40.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.athleteName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${entry.rawScore} ${entry.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    entry.isImproved?.let { improved ->
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            if (improved) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = if (improved) PerformanceGreenText else PerformanceRedText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // PERCENTILE & ZONE
            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    color = bgColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = classification,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
                entry.percentile?.let {
                    Text(
                        "${it}th Percentile",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun RemediationFlagCard(
    flag: RemediationFlag,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PerformanceRed)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    flag.athleteName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = PerformanceRedText
                )
                Text(
                    "${flag.testName} - ${flag.rawScore} ${flag.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PerformanceRedText.copy(alpha = 0.8f)
                )
                flag.classification?.let { cls ->
                    Text(
                        cls,
                        style = MaterialTheme.typography.bodySmall,
                        color = PerformanceRedText.copy(alpha = 0.7f)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .background(PerformanceRedBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    "${flag.percentile}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = PerformanceRedText,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
