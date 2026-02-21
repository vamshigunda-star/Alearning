package com.example.alearning.ui.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.usecase.testing.LeaderboardEntry
import com.example.alearning.ui.theme.*

@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LeaderboardContent(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                is LeaderboardAction.OnNavigateBack -> onNavigateBack()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardContent(
    uiState: LeaderboardUiState,
    onAction: (LeaderboardAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.mode == "event") "Event Leaderboard" else "All-Time Leaderboard"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(LeaderboardAction.OnNavigateBack) }) {
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
            uiState.errorMessage != null && uiState.tests.isEmpty() -> {
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
                        TextButton(onClick = { onAction(LeaderboardAction.OnDismissError) }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
            uiState.tests.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tests available")
                }
            }
            else -> {
                LeaderboardBody(uiState = uiState, onAction = onAction, padding = padding)
            }
        }
    }
}

@Composable
private fun LeaderboardBody(
    uiState: LeaderboardUiState,
    onAction: (LeaderboardAction) -> Unit,
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Test selection tabs
        ScrollableTabRow(
            selectedTabIndex = uiState.tests.indexOfFirst { it.id == uiState.selectedTestId }.coerceAtLeast(0)
        ) {
            uiState.tests.forEach { test ->
                Tab(
                    selected = test.id == uiState.selectedTestId,
                    onClick = { onAction(LeaderboardAction.OnSelectTest(test.id)) },
                    text = { Text(test.name, maxLines = 1) }
                )
            }
        }

        // Leaderboard entries
        val leaderboard = uiState.leaderboard
        if (leaderboard == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (leaderboard.entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No results for this test")
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(leaderboard.entries) { entry ->
                    LeaderboardEntryRow(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardEntryRow(entry: LeaderboardEntry) {
    val (bgColor, textColor) = when {
        entry.percentile == null -> PerformanceGrey to PerformanceGreyText
        entry.percentile >= 60 -> PerformanceGreen to PerformanceGreenText
        entry.percentile >= 30 -> PerformanceYellow to PerformanceYellowText
        else -> PerformanceRed to PerformanceRedText
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rank
            Box(
                modifier = Modifier.width(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (entry.rank <= 3) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Rank ${entry.rank}",
                        tint = when (entry.rank) {
                            1 -> SportOrange
                            2 -> OutlineGrey
                            else -> SportOrange.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text(
                        "#${entry.rank}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.athleteName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${entry.rawScore} ${entry.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    entry.classification?.let { cls ->
                        Text(
                            " - $cls",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            entry.isImproved?.let { improved ->
                Icon(
                    if (improved) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = if (improved) "Improved" else "Declined",
                    tint = if (improved) PerformanceGreenText else PerformanceRedText,
                    modifier = Modifier.size(20.dp)
                )
            }

            entry.percentile?.let { p ->
                Box(
                    modifier = Modifier
                        .background(bgColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${p}%",
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
