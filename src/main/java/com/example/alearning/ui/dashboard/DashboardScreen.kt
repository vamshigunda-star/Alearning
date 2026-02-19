package com.example.alearning.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.ui.theme.NavyPrimary
import com.example.alearning.ui.theme.NavyVariant
import com.example.alearning.ui.theme.SportBlue
import com.example.alearning.ui.theme.SportOrange
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    onNavigateToRoster: () -> Unit,
    onNavigateToTestLibrary: () -> Unit,
    onNavigateToCreateEvent: () -> Unit,
    onNavigateToQuickTest: () -> Unit,
    onNavigateToTestingGrid: (String, String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    DashboardContent(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                is DashboardAction.OnCreateEventClick -> onNavigateToCreateEvent()
                is DashboardAction.OnQuickTestClick -> onNavigateToQuickTest()
                is DashboardAction.OnRosterClick -> onNavigateToRoster()
                is DashboardAction.OnTestLibraryClick -> onNavigateToTestLibrary()
                is DashboardAction.OnEventClick -> onNavigateToTestingGrid(action.eventId, action.groupId)
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    uiState: DashboardUiState,
    onAction: (DashboardAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ALearning",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { onAction(DashboardAction.OnDismissError) }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
            else -> {
                DashboardBody(uiState = uiState, onAction = onAction, padding = padding)
            }
        }
    }
}

@Composable
private fun DashboardBody(
    uiState: DashboardUiState,
    onAction: (DashboardAction) -> Unit,
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp)
    ) {
        item {
            HeroCard(
                title = "Start Testing Event",
                subtitle = "Create a testing event for a group",
                icon = Icons.Default.Add,
                onClick = { onAction(DashboardAction.OnCreateEventClick) }
            )
        }

        item {
            Text(
                "Manage",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardTile(
                        title = "Roster",
                        icon = Icons.Default.Group,
                        color = SportBlue,
                        onClick = { onAction(DashboardAction.OnRosterClick) },
                        modifier = Modifier.weight(1f)
                    )
                    DashboardTile(
                        title = "Test Library",
                        icon = Icons.AutoMirrored.Filled.LibraryBooks,
                        color = NavyVariant,
                        onClick = { onAction(DashboardAction.OnTestLibraryClick) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    DashboardTile(
                        title = "Quick Test",
                        icon = Icons.Default.Bolt,
                        color = SportOrange,
                        onClick = { onAction(DashboardAction.OnQuickTestClick) },
                        modifier = Modifier.weight(1f)
                    )
                    DashboardTile(
                        title = "Analytics",
                        icon = Icons.Default.BarChart,
                        color = NavyVariant,
                        onClick = { /* TODO */ },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Text(
                "Recent Events",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (uiState.recentEvents.isEmpty()) {
            item {
                EmptyStateCard()
            }
        } else {
            items(uiState.recentEvents) { event ->
                val groupName = uiState.groups
                    .find { it.id == event.groupId }?.name ?: "No group"
                EventCard(
                    eventName = event.name,
                    date = event.date,
                    groupName = groupName,
                    onClick = {
                        event.groupId?.let { groupId ->
                            onAction(DashboardAction.OnEventClick(event.id, groupId))
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HeroCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = SportOrange)
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.2f), shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardTile(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun EventCard(
    eventName: String,
    date: Long,
    groupName: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(NavyPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = NavyPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eventName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                Text(
                    text = dateFormat.format(Date(date)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "No recent events",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Your testing history will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
