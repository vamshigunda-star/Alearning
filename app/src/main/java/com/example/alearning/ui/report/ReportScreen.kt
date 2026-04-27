package com.example.alearning.ui.report

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.people.Group
import com.example.alearning.domain.model.testing.TestingEvent
import com.example.alearning.ui.theme.NavyPrimary
import com.example.alearning.ui.theme.PerformanceRedText
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    onNavigateToGroupReport: (String, String?) -> Unit,
    viewModel: ReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Summary Card
                item {
                    NeedsAttentionCard(count = uiState.needsAttentionCount)
                }

                // Groups Section
                item {
                    SectionHeader(title = "Browse by Group", icon = Icons.Default.Group)
                }

                if (uiState.groups.isEmpty()) {
                    item { Text("No groups found", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) }
                } else {
                    items(uiState.groups) { group ->
                        GroupItem(
                            group = group,
                            onClick = { 
                                onNavigateToGroupReport(group.id, null)
                            }
                        )
                    }
                }

                // Recent Events Section
                item {
                    SectionHeader(title = "Event Archive", icon = Icons.Default.History)
                }

                if (uiState.recentEvents.isEmpty()) {
                    item { Text("No past events", style = MaterialTheme.typography.bodyMedium, color = Color.Gray) }
                } else {
                    items(uiState.recentEvents) { event ->
                        EventArchiveItem(
                            event = event,
                            onClick = { 
                                event.groupId?.let { onNavigateToGroupReport(event.id, it) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NeedsAttentionCard(count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (count > 0) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = if (count > 0) Icons.Default.Warning else Icons.Default.Group,
                contentDescription = null,
                tint = if (count > 0) PerformanceRedText else NavyPrimary
            )
            Column {
                Text(
                    text = if (count > 0) "$count Athletes Need Attention" else "All athletes in Healthy Zone",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (count > 0) PerformanceRedText else NavyPrimary
                )
                Text(
                    text = "Based on most recent 30th percentile threshold",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = NavyPrimary)
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun GroupItem(group: Group, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, fontWeight = FontWeight.Bold)
                // Add member count here if available in domain model
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
private fun EventArchiveItem(event: TestingEvent, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(event.name, fontWeight = FontWeight.Bold)
                Text(dateFormat.format(Date(event.date)), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF0F0F0))
    }
}
