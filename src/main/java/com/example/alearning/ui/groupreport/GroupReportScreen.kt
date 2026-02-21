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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.usecase.testing.GroupLeaderboard
import com.example.alearning.domain.usecase.testing.LeaderboardEntry
import com.example.alearning.domain.usecase.testing.RemediationFlag
import com.example.alearning.domain.usecase.testing.RemediationList
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
        eventId = viewModel.eventId,
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
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null && uiState.event == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { onAction(GroupReportAction.OnDismissError) }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
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
        // Event summary card
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "${uiState.tests.size} tests completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        // Test selection tabs for leaderboard
        if (uiState.tests.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Leaderboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        onAction(GroupReportAction.OnNavigateToLeaderboard(eventId, groupId, "event"))
                    }) {
                        Text("Full View")
                    }
                }
            }

            item {
                ScrollableTabRow(
                    selectedTabIndex = uiState.tests.indexOfFirst { it.id == uiState.selectedTestId }.coerceAtLeast(0)
                ) {
                    uiState.tests.forEach { test ->
                        Tab(
                            selected = test.id == uiState.selectedTestId,
                            onClick = { onAction(GroupReportAction.OnSelectTest(test.id)) },
                            text = { Text(test.name, maxLines = 1) }
                        )
                    }
                }
            }

            // Leaderboard results
            if (uiState.isLeaderboardLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            } else {
                uiState.leaderboard?.let { leaderboard ->
                    items(leaderboard.entries.take(10)) { entry ->
                        LeaderboardEntryCard(
                            entry = entry,
                            onClick = { onAction(GroupReportAction.OnNavigateToAthleteReport(entry.individualId)) }
                        )
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
        entry.percentile >= 60 -> PerformanceGreen to PerformanceGreenText
        entry.percentile >= 30 -> PerformanceYellow to PerformanceYellowText
        else -> PerformanceRed to PerformanceRedText
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rank
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (entry.rank <= 3) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = when (entry.rank) {
                            1 -> SportOrange
                            2 -> OutlineGrey
                            else -> SportOrange.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        "#${entry.rank}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(entry.athleteName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(
                    "${entry.rawScore} ${entry.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            entry.isImproved?.let { improved ->
                Icon(
                    if (improved) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (improved) PerformanceGreenText else PerformanceRedText,
                    modifier = Modifier.size(16.dp)
                )
            }

            entry.percentile?.let { p ->
                Box(
                    modifier = Modifier
                        .background(bgColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("${p}%", style = MaterialTheme.typography.labelMedium, color = textColor, fontWeight = FontWeight.Bold)
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
