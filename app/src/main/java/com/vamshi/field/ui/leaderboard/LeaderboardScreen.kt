package com.vamshi.field.ui.leaderboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import com.vamshi.field.domain.usecase.testing.LeaderboardEntry
import com.vamshi.field.ui.components.AppTopBar
import com.vamshi.field.ui.theme.OutlineGrey
import com.vamshi.field.ui.theme.PerformanceGreen
import com.vamshi.field.ui.theme.PerformanceGreenText
import com.vamshi.field.ui.theme.PerformanceGrey
import com.vamshi.field.ui.theme.PerformanceGreyText
import com.vamshi.field.ui.theme.PerformanceRed
import com.vamshi.field.ui.theme.PerformanceRedText
import com.vamshi.field.ui.theme.PerformanceYellow
import com.vamshi.field.ui.theme.PerformanceYellowText
import com.vamshi.field.ui.theme.SportOrange
import com.vamshi.field.ui.theme.SportOrangeContainer

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

@Composable
fun LeaderboardContent(
    uiState: LeaderboardUiState,
    onAction: (LeaderboardAction) -> Unit
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = if (uiState.mode == "event") "Event Leaderboard" else "All-Time Leaderboard",
                navigationIcon = {
                    IconButton(onClick = { onAction(LeaderboardAction.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState()
            uiState.errorMessage != null && uiState.tests.isEmpty() -> ErrorState(
                message = uiState.errorMessage,
                onDismiss = { onAction(LeaderboardAction.OnDismissError) }
            )
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
            LoadingState()
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

/**
 * Visual ranking hierarchy: Top 5 get the strongest emphasis, ranks 6-10 get
 * moderate emphasis, everything below is the standard leaderboard row.
 */
private enum class RankTier { TOP_5, TOP_10, STANDARD }

private fun tierFor(rank: Int): RankTier = when {
    rank <= 5 -> RankTier.TOP_5
    rank <= 10 -> RankTier.TOP_10
    else -> RankTier.STANDARD
}

@Composable
private fun LeaderboardEntryRow(entry: LeaderboardEntry) {
    val (bgColor, textColor) = when {
        entry.percentile == null -> PerformanceGrey to PerformanceGreyText
        entry.percentile >= 60 -> PerformanceGreen to PerformanceGreenText
        entry.percentile >= 30 -> PerformanceYellow to PerformanceYellowText
        else -> PerformanceRed to PerformanceRedText
    }

    val tier = tierFor(entry.rank)

    val cardElevation = when (tier) {
        RankTier.TOP_5 -> 8.dp
        RankTier.TOP_10 -> 3.dp
        RankTier.STANDARD -> 1.dp
    }
    val cardBorder = when (tier) {
        RankTier.TOP_5 -> BorderStroke(2.dp, SportOrange)
        RankTier.TOP_10 -> BorderStroke(1.dp, SportOrange.copy(alpha = 0.4f))
        RankTier.STANDARD -> null
    }
    val cardContainerColor = when (tier) {
        RankTier.TOP_5 -> SportOrangeContainer
        RankTier.TOP_10 -> MaterialTheme.colorScheme.surface
        RankTier.STANDARD -> MaterialTheme.colorScheme.surface
    }
    val rowPadding = if (tier == RankTier.TOP_5) 20.dp else 16.dp
    val rankBoxSize = when (tier) {
        RankTier.TOP_5 -> 48.dp
        RankTier.TOP_10 -> 40.dp
        RankTier.STANDARD -> 40.dp
    }
    val nameStyle = when (tier) {
        RankTier.TOP_5 -> MaterialTheme.typography.titleMedium
        RankTier.TOP_10 -> MaterialTheme.typography.bodyLarge
        RankTier.STANDARD -> MaterialTheme.typography.bodyLarge
    }
    val nameWeight = when (tier) {
        RankTier.TOP_5 -> FontWeight.Bold
        RankTier.TOP_10 -> FontWeight.SemiBold
        RankTier.STANDARD -> FontWeight.Medium
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(rowPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rank
            Box(
                modifier = if (tier == RankTier.TOP_5) {
                    Modifier.size(rankBoxSize).background(SportOrange, CircleShape)
                } else {
                    Modifier.width(rankBoxSize)
                },
                contentAlignment = Alignment.Center
            ) {
                if (entry.rank <= 3) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Rank ${entry.rank}",
                        tint = when {
                            tier == RankTier.TOP_5 -> Color.White
                            entry.rank == 1 -> SportOrange
                            entry.rank == 2 -> OutlineGrey
                            else -> SportOrange.copy(alpha = 0.6f)
                        },
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text(
                        "#${entry.rank}",
                        style = if (tier == RankTier.TOP_5) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (tier == RankTier.TOP_5) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.athleteName,
                    style = nameStyle,
                    fontWeight = nameWeight
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
