package com.example.alearning.ui.report

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.reports.AthleteDashboardData
import com.example.alearning.domain.model.reports.AthleteTestTile
import com.example.alearning.domain.model.reports.Classification
import com.example.alearning.domain.model.reports.LeaderboardRow
import com.example.alearning.domain.model.reports.RecentSessionRow
import com.example.alearning.domain.model.reports.SessionReportData
import com.example.alearning.reports.components.ZoneChip
import com.example.alearning.reports.components.zoneColors
import com.example.alearning.ui.components.AppTopBar
import com.example.alearning.ui.components.AppTopBarSubtitleColor
import com.example.alearning.ui.components.charts.HorizontalBarChart
import com.example.alearning.ui.theme.NavyPrimary
import com.example.alearning.ui.theme.PerformanceGreen
import com.example.alearning.ui.theme.PerformanceGreenText
import com.example.alearning.ui.theme.PerformanceRed
import com.example.alearning.ui.theme.PerformanceRedText
import com.example.alearning.ui.theme.SportOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────
// Entry Point
// ─────────────────────────────────────────────────────────────────

@Composable
fun ReportScreen(
    onNavigateToGroup: (String) -> Unit,
    onNavigateToSession: (String, String) -> Unit,
    onNavigateToAthlete: (String) -> Unit,
    viewModel: ReportsHubViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = {
                    Column {
                        Text("Reports", style = MaterialTheme.typography.titleLarge)
                        Text("Performance insights", style = MaterialTheme.typography.labelSmall, color = AppTopBarSubtitleColor)
                    }
                }
            )
        }
    ) { padding ->
        ReportsHubContent(
            uiState = uiState,
            onAction = { action ->
                when (action) {
                    is ReportsHubAction.SelectAthlete -> {
                        viewModel.onAction(action)
                        onNavigateToAthlete(action.id)
                    }
                    is ReportsHubAction.SelectEvent -> {
                        viewModel.onAction(action)
                        onNavigateToSession(action.groupId, action.eventId)
                    }
                    else -> viewModel.onAction(action)
                }
            },
            modifier = Modifier.padding(padding)
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Hub Content with Two Tabs
// ─────────────────────────────────────────────────────────────────

@Composable
private fun ReportsHubContent(
    uiState: ReportsHubUiState,
    onAction: (ReportsHubAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabTitles = listOf("Athlete Profile", "Event Report")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = NavyPrimary,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = SportOrange
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) SportOrange else Color.White.copy(alpha = 0.7f)
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> AthleteProfileTab(uiState = uiState, onAction = onAction)
            1 -> EventReportTab(uiState = uiState, onAction = onAction)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// TAB 1: Athlete Profile
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AthleteProfileTab(
    uiState: ReportsHubUiState,
    onAction: (ReportsHubAction) -> Unit
) {
    val data = uiState.homeData
    val athleteData = uiState.athleteData

    if (uiState.isLoadingHome) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    if (data == null || data.groups.isEmpty()) {
        EmptyState(
            icon = "👤",
            title = "No Athletes Yet",
            subtitle = "Add athletes to a group using the Roster tab to see their performance profiles here."
        )
        return
    }

    // Build all individuals from flags + groups for the picker
    val allAthletes = data.flags.map { it.individualId to it.athleteName }.distinctBy { it.first }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Athlete Picker ──
        item {
            if (allAthletes.isNotEmpty()) {
                AthletePickerRow(
                    athletes = allAthletes,
                    selectedId = uiState.selectedAthleteId,
                    onSelect = { onAction(ReportsHubAction.SelectAthlete(it)) }
                )
            } else {
                // Show groups as entry points when no flags exist
                Text(
                    "Select an athlete from the Roster tab to view their profile.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (uiState.isLoadingAthlete) {
            item {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            return@LazyColumn
        }

        if (athleteData == null) {
            item {
                EmptyCard("Tap an athlete card above to load their performance profile.")
            }
            return@LazyColumn
        }

        // ── Athlete Meta Card ──
        item { AthleteMetaCard(athleteData = athleteData) }

        // ── Fitness Radar Summary (Zone overview) ──
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = NavyPrimary)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Overall Fitness Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Performance zones across all completed tests",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(16.dp))
                    // Zone distribution bar
                    ZoneDistributionBar(tiles = athleteData.tiles)
                    Spacer(Modifier.height(12.dp))
                    // Zone legend row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val superior = athleteData.tiles.count { it.classification == Classification.SUPERIOR }
                        val healthy = athleteData.tiles.count { it.classification == Classification.HEALTHY }
                        val needs = athleteData.tiles.count { it.classification == Classification.NEEDS_IMPROVEMENT }
                        ZoneLegendItem("Superior", superior, Color(0xFF4CAF50))
                        ZoneLegendItem("Healthy", healthy, Color(0xFFFFC107))
                        ZoneLegendItem("Needs Work", needs, Color(0xFFF44336))
                    }
                }
            }
        }

        // ── Test Tiles ──
        item {
            Text(
                "Recent Assessment Scores",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Tap a card to highlight it — navigate to the athlete profile for full history",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (athleteData.tiles.isEmpty()) {
            item { EmptyCard("No test results recorded yet for this athlete.") }
        } else {
            items(athleteData.tiles, key = { it.test.id }) { tile ->
                AthleteTestTileCard(
                    tile = tile,
                    isSelected = tile.test.id == uiState.selectedAthleteTestId,
                    onClick = { onAction(ReportsHubAction.SelectAthleteTest(tile.test.id)) }
                )
            }
        }

        // ── Flags / Alerts ──
        val athleteFlags = athleteData.flags
        if (athleteFlags.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(athleteFlags, key = { "${it.individualId}-${it.type}" }) { flag ->
                AlertCard(message = flag.message)
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────
// TAB 2: Event Report
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventReportTab(
    uiState: ReportsHubUiState,
    onAction: (ReportsHubAction) -> Unit
) {
    val data = uiState.homeData
    val eventData = uiState.eventData

    if (uiState.isLoadingHome) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val sessions = data?.recentSessions ?: emptyList()
    if (sessions.isEmpty()) {
        EmptyState(
            icon = "📋",
            title = "No Testing Events",
            subtitle = "Create a testing event from the Home screen, record results, and they'll appear here for analysis."
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Event Picker ──
        item {
            EventPickerRow(
                sessions = sessions,
                selectedEventId = uiState.selectedEventId,
                onSelect = { row ->
                    val gid = row.groupId ?: return@EventPickerRow
                    onAction(ReportsHubAction.SelectEvent(row.event.id, gid))
                }
            )
        }

        if (uiState.isLoadingEvent) {
            item {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            return@LazyColumn
        }

        if (eventData == null) {
            item { EmptyCard("Select an event above to view the full report.") }
            return@LazyColumn
        }

        // ── Event Stats Header ──
        item { EventStatsHeader(eventData = eventData) }

        // ── Test Selector Tabs ──
        if (eventData.tests.isNotEmpty()) {
            item {
                Text("View Test Results:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(eventData.tests, key = { it.id }) { test ->
                        FilterChip(
                            selected = test.id == uiState.selectedEventTestId,
                            onClick = { onAction(ReportsHubAction.SelectEventTest(test.id)) },
                            label = {
                                Text(
                                    test.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SportOrange,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            val activeTestId = uiState.selectedEventTestId ?: eventData.tests.firstOrNull()?.id
            val activeRows = activeTestId?.let { eventData.leaderboardByTest[it] }.orEmpty()
            val activeTest = eventData.tests.find { it.id == activeTestId }

            // ── Roster Comparison Bar Chart ──
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(
                            "Team Results Comparison",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Bars colored by zone: Green ≥60, Yellow 30–59, Red <30",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        HorizontalBarChart(rows = activeRows)
                    }
                }
            }

            // ── Leaderboard ──
            item {
                Text("Leaderboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (activeRows.isEmpty()) {
                item { EmptyCard("No results recorded for this test yet.") }
            } else {
                items(activeRows, key = { it.individualId }) { row ->
                    LeaderboardCard(row = row)
                }
            }

            // ── Stats Summary ──
            val validScores = activeRows.mapNotNull { it.rawScore }
            if (validScores.isNotEmpty()) {
                val maxVal = validScores.max()
                val minVal = validScores.min()
                val avgVal = validScores.average()
                val unit = activeRows.firstOrNull()?.unit ?: ""

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text("Standard Stats Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "Calculated across all participants in this event",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatItem("Maximum", maxVal, unit, Modifier.weight(1f))
                                StatItem("Average", avgVal, unit, Modifier.weight(1f))
                                StatItem("Minimum", minVal, unit, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // ── Coaching Interpretation ──
            if (activeTest != null) {
                val interpretationText = when {
                    activeTest.name.contains("Jump", ignoreCase = true) || activeTest.categoryId.contains("power", ignoreCase = true) ->
                        "Vertical jump is a primary indicator of lower-body explosive power. Training should focus on plyometrics, Olympic lifting, and power cleans. Athletes in the Red zone need core strength and landing mechanics coaching."
                    activeTest.name.contains("Sprint", ignoreCase = true) || activeTest.categoryId.contains("speed", ignoreCase = true) ->
                        "40m sprint tests acceleration and top-end speed. Focus on sprint mechanics, drive phase alignment, and hamstring strength. Red zone athletes require stride frequency and acceleration work."
                    activeTest.name.contains("Squat", ignoreCase = true) || activeTest.categoryId.contains("strength", ignoreCase = true) ->
                        "1RM Squat is the baseline for lower body absolute strength. Focus on progressive overload in squat variations. Red zone athletes need technique checks and core stabilization before heavy lifting."
                    activeTest.name.contains("Beep", ignoreCase = true) || activeTest.categoryId.contains("endurance", ignoreCase = true) ->
                        "Beep test measures aerobic capacity and fatigue resistance. Focus on high-intensity interval training (HIIT) and aerobic base building. Red zone athletes require low-intensity steady-state (LISS) conditioning."
                    activeTest.name.contains("Agility", ignoreCase = true) || activeTest.categoryId.contains("agility", ignoreCase = true) ->
                        "Pro Agility (5-10-5) tests lateral acceleration, change-of-direction speed, and deceleration. Focus on center of gravity and footwork. Red zone athletes need deceleration mechanics drills."
                    else -> "Assessment benchmarks fitness standards against age-and-sex norms. Athletes in needs-improvement zones require foundational corrective training."
                }

                val redZone = activeRows.filter { it.percentile != null && it.percentile < 30 }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(
                                "Result Interpretation & Guidance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                interpretationText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (redZone.isNotEmpty()) {
                                Spacer(Modifier.height(16.dp))
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = PerformanceRed.copy(alpha = 0.15f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, PerformanceRed, RoundedCornerShape(8.dp))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = null, tint = PerformanceRedText)
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Remediation Required (Scored < 30th Percentile)",
                                                color = PerformanceRedText,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Spacer(Modifier.height(6.dp))
                                        val names = redZone.joinToString(", ") {
                                            val v = if (it.rawScore != null && it.rawScore % 1.0 == 0.0)
                                                it.rawScore.toInt().toString()
                                            else String.format("%.1f", it.rawScore)
                                            "${it.athleteName} ($v ${it.unit})"
                                        }
                                        Text(
                                            "Focus corrective training on: $names.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ─────────────────────────────────────────────────────────────────
// Sub-Components
// ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AthletePickerRow(
    athletes: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = athletes.firstOrNull { it.first == selectedId }?.second ?: ""
    var searchText by remember { mutableStateOf(selectedName) }

    // Update searchText when the selected athlete changes externally
    LaunchedEffect(selectedName) {
        if (selectedName.isNotEmpty() && !expanded) {
            searchText = selectedName
        }
    }

    val filteredAthletes = if (searchText.isBlank() || (searchText == selectedName && !expanded)) {
        athletes
    } else {
        athletes.filter { it.second.contains(searchText, ignoreCase = true) }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = searchText.ifEmpty { if (!expanded) "Select Athlete" else "" },
            onValueChange = { 
                searchText = it
                expanded = true 
            },
            readOnly = false,
            label = { Text("Select Athlete") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable)
        )
        if (filteredAthletes.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                filteredAthletes.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            searchText = name
                            onSelect(id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventPickerRow(
    sessions: List<RecentSessionRow>,
    selectedEventId: String?,
    onSelect: (RecentSessionRow) -> Unit
) {
    val df = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    var expanded by remember { mutableStateOf(false) }
    val selectedRow = sessions.firstOrNull { it.event.id == selectedEventId }
    val displayText = selectedRow?.let {
        "${it.event.name} — ${df.format(Date(it.event.date))}"
    } ?: "Select Testing Event"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = displayText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Select Testing Event") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sessions.forEach { row ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(row.event.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${row.groupName ?: ""} · ${df.format(Date(row.event.date))}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        onSelect(row)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AthleteMetaCard(athleteData: AthleteDashboardData) {
    val df = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val athlete = athleteData.athlete
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(SportOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = athlete.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(athlete.fullName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "${athleteData.groups.firstOrNull()?.name ?: "—"} · Age ${athlete.currentAge}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
                if (!athlete.medicalAlert.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "⚠ ${athlete.medicalAlert}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFCC02)
                    )
                }
            }
            // Session avg percentile badge
            athleteData.athleteSessionAvgPctile?.let { avg ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$avg",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = SportOrange
                    )
                    Text("avg %ile", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
private fun ZoneDistributionBar(tiles: List<AthleteTestTile>) {
    val total = tiles.size.coerceAtLeast(1).toFloat()
    val superior = tiles.count { it.classification == Classification.SUPERIOR } / total
    val healthy = tiles.count { it.classification == Classification.HEALTHY } / total
    val needs = tiles.count { it.classification == Classification.NEEDS_IMPROVEMENT } / total
    val noData = tiles.count { it.classification == Classification.NO_DATA } / total

    Row(
        modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))
    ) {
        if (superior > 0) Box(Modifier.weight(superior).fillMaxHeight().background(Color(0xFF4CAF50)))
        if (healthy > 0) Box(Modifier.weight(healthy).fillMaxHeight().background(Color(0xFFFFC107)))
        if (needs > 0) Box(Modifier.weight(needs).fillMaxHeight().background(Color(0xFFF44336)))
        if (noData > 0) Box(Modifier.weight(noData).fillMaxHeight().background(Color(0xFF9E9E9E)))
    }
}

@Composable
private fun ZoneLegendItem(label: String, count: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text("$label ($count)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
    }
}

@Composable
private fun AthleteTestTileCard(
    tile: AthleteTestTile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = zoneColors(tile.classification)
    val latest = tile.latestResult
    val scoreStr = if (latest != null) {
        val s = latest.rawScore
        (if (s % 1.0 == 0.0) s.toInt().toString() else String.format("%.1f", s)) + " ${tile.test.unit}"
    } else "No Data"

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(2.dp, SportOrange, RoundedCornerShape(12.dp))
                else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = colors.bg),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(tile.test.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = colors.fg)
                Text(scoreStr, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = colors.fg)
            }
            Column(horizontalAlignment = Alignment.End) {
                ZoneChip(classification = tile.classification)
                latest?.percentile?.let { pct ->
                    Spacer(Modifier.height(4.dp))
                    Text("${pct}th %ile", style = MaterialTheme.typography.labelSmall, color = colors.fg)
                }
            }
        }
    }
}

@Composable
private fun EventStatsHeader(eventData: SessionReportData) {
    val df = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val allRows = eventData.leaderboardByTest.values.flatten()
    val avgPctile = allRows.mapNotNull { it.percentile }.let { pctiles ->
        if (pctiles.isEmpty()) null else pctiles.average().toInt()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavyPrimary)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(eventData.event.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                "${eventData.group.name} · ${df.format(Date(eventData.event.date))}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                EventStatItem("Event Date", df.format(Date(eventData.event.date)).split(",").firstOrNull() ?: "—")
                EventStatItem("Athletes", "${eventData.athletesTested}/${eventData.totalAthletes}")
                EventStatItem("Tests", "${eventData.tests.size}")
                EventStatItem("Team Avg", avgPctile?.let { "${it}%ile" } ?: "—")
            }
        }
    }
}

@Composable
private fun EventStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = SportOrange)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun LeaderboardCard(row: LeaderboardRow) {
    val colors = zoneColors(row.classification)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Rank badge
            Box(
                modifier = Modifier.size(32.dp).background(
                    if (row.rank <= 3) SportOrange else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "#${row.rank}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (row.rank <= 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                row.athleteName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val scoreStr = if (row.rawScore != null) {
                val s = row.rawScore
                (if (s % 1.0 == 0.0) s.toInt().toString() else String.format("%.1f", s)) + " ${row.unit}"
            } else "—"
            Text(scoreStr, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

            ZoneChip(classification = row.classification)
        }
    }
}

@Composable
private fun StatItem(label: String, value: Double, unit: String, modifier: Modifier = Modifier) {
    val valueStr = if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("$valueStr $unit", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AlertCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = PerformanceRed.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = PerformanceRedText, modifier = Modifier.size(20.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = PerformanceRedText, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun EmptyState(icon: String, title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(icon, style = MaterialTheme.typography.displayMedium)
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
