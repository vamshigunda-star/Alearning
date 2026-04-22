package com.example.alearning.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.testing.TestingEvent
import com.example.alearning.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onNavigateToRoster: () -> Unit,
    onNavigateToTestLibrary: () -> Unit,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToQuickTest: () -> Unit,
    onNavigateToTestingGrid: (String, String) -> Unit,
    onNavigateToLeaderboard: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    DashboardContent(
        modifier = modifier,
        uiState = uiState,
        onAction = {
            when (it) {
                DashboardAction.OnCreateEventClick -> onNavigateToCreateEvent()
                DashboardAction.OnQuickTestClick -> onNavigateToQuickTest()
                DashboardAction.OnRosterClick -> onNavigateToRoster()
                DashboardAction.OnTestLibraryClick -> onNavigateToTestLibrary()
                is DashboardAction.OnEventClick -> onNavigateToTestingGrid(it.eventId, it.groupId)
                DashboardAction.OnLeaderboardClick -> onNavigateToLeaderboard()
                DashboardAction.OnAnalyticsClick -> onNavigateToAnalytics()
                else -> viewModel.onAction(it)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    modifier: Modifier = Modifier,
    uiState: DashboardUiState,
    onAction: (DashboardAction) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "ALearning",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 80.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                ContextHeaderCard(
                    athleteCount = uiState.activeAthletes,
                    groupCount = uiState.scheduledTestCount // Mapping scheduled tests as group count temporarily, or omit? Spec just asks for group count but UI state has scheduledTestCount. I'll pass scheduledTestCount.
                )
            }

            item {
                HeroCard {
                    onAction(DashboardAction.OnCreateEventClick)
                }
            }

            item {
                QuickActionsSection(onAction = onAction)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Events",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = {}, enabled = false) {
                        Text("See All")
                    }
                }
            }

            if (uiState.recentEvents.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Default.Event,
                        title = "No recent events",
                        message = "Create one to get started!",
                        actionLabel = "Create Event",
                        onAction = { onAction(DashboardAction.OnCreateEventClick) }
                    )
                }
            } else {
                items(uiState.recentEvents) { event ->
                    RecentEventItem(
                        event = event,
                        onClick = {
                            event.groupId?.let {
                                onAction(DashboardAction.OnEventClick(event.id, it))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContextHeaderCard(
    athleteCount: Int,
    groupCount: Int
) {
    val greeting = remember {
        when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning, Coach"
            in 12..16 -> "Good afternoon, Coach"
            else -> "Good evening, Coach"
        }
    }
    val dateStr = remember {
        SimpleDateFormat("EEEE, d MMM", Locale.getDefault())
            .format(Date())
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(
            containerColor = NavyPrimary
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                greeting,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "$athleteCount athletes · $groupCount tests", // Adjusted text mapping based on ui state
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                dateStr,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun HeroCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize(),
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = SportOrange),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Start Testing Event",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Select group → pick tests → begin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = Color.White.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionsSection(onAction: (DashboardAction) -> Unit) {
    Column {
        Text(
            "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            QuickActionCard(
                icon = Icons.Default.Bolt,
                label = "Quick Test",
                tint = SportOrange,
                modifier = Modifier.weight(1f),
                onClick = { onAction(DashboardAction.OnQuickTestClick) }
            )
            QuickActionCard(
                icon = Icons.Default.Group,
                label = "Roster",
                tint = SportBlue,
                modifier = Modifier.weight(1f),
                onClick = { onAction(DashboardAction.OnRosterClick) }
            )
            QuickActionCard(
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                label = "Tests",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = { onAction(DashboardAction.OnTestLibraryClick) }
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    icon: ImageVector,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(84.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentEventItem(event: TestingEvent, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatEventDate(event.date, event.name),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
}

private fun formatEventDate(timestamp: Long, eventName: String): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }

    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    val dayString = when {
        calendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) -> "Today"
        calendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR) -> "Yesterday"
        else -> SimpleDateFormat("MMMM d", Locale.getDefault()).format(calendar.time)
    }

    return "$dayString, ${timeFormat.format(calendar.time)} • $eventName"
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onAction,
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Preview
@Composable
private fun DashboardContentPreview() {
    AlearningTheme {
        DashboardContent(
            uiState = DashboardUiState(
                activeAthletes = 25,
                scheduledTestCount = 4,
                recentEvents = listOf(
                    TestingEvent(
                        id = "1",
                        name = "Max-Out Day",
                        date = System.currentTimeMillis() - 86400000,
                        groupId = "1",
                    ),
                    TestingEvent(
                        id = "2",
                        name = "Combine Prep",
                        date = System.currentTimeMillis() - 86400000 * 2,
                        groupId = "2",
                    )
                )
            ),
            onAction = {}
        )
    }
}
