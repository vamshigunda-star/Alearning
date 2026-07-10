package com.example.alearning.ui.session

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import com.example.alearning.domain.model.reports.LeaderboardRow
import com.example.alearning.reports.components.AthleteLeaderRow
import com.example.alearning.reports.components.SessionPill
import com.example.alearning.reports.components.SessionSwitcherSheet
import com.example.alearning.reports.components.ZoneChip
import com.example.alearning.domain.model.reports.Classification
import com.example.alearning.ui.components.AppTopBar
import com.example.alearning.ui.components.AppTopBarSubtitleColor
import com.example.alearning.ui.theme.PerformanceRed
import com.example.alearning.ui.theme.PerformanceRedText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.alearning.util.CsvExporter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.border
import com.example.alearning.ui.components.charts.HorizontalBarChart

import com.example.alearning.ui.aicoach.AiCoachViewModel
import com.example.alearning.domain.repository.AiCoachStatus
import com.example.alearning.ui.aicoach.components.AiFloatingActionButton

@Composable
fun SessionReportScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAthlete: (String, String) -> Unit,
    onResumeTesting: (String, String) -> Unit,
    onNavigateToAiCoach: (String?) -> Unit,
    viewModel: SessionReportViewModel = hiltViewModel(),
    aiCoachViewModel: AiCoachViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sessionId = uiState.data?.event?.id ?: ""
    val groupId = uiState.data?.group?.id ?: ""

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    LaunchedEffect(viewModel.exportEvent) {
        viewModel.exportEvent.collect { request ->
            when (request) {
                is SessionReportViewModel.ExportRequest.Event -> {
                    CsvExporter.exportEventResults(context, request.eventName, request.results, request.tests)
                }
            }
        }
    }

    SessionReportContent(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                SessionReportAction.OnNavigateBack -> onNavigateBack()
                is SessionReportAction.OnNavigateToAthlete ->
                    onNavigateToAthlete(action.individualId, sessionId)
                SessionReportAction.OnResumeTesting -> {
                    if (sessionId.isNotEmpty() && groupId.isNotEmpty()) {
                        onResumeTesting(sessionId, groupId)
                    }
                }
                else -> viewModel.onAction(action)
            }
        },
        aiCoachViewModel = aiCoachViewModel,
        onNavigateToAiCoach = onNavigateToAiCoach
    )

    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onAction(SessionReportAction.OnDismissDelete) },
            title = { Text("Delete Event?") },
            text = { Text("Are you sure you want to permanently delete this event and all associated test results? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.onAction(SessionReportAction.OnConfirmDelete) }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(SessionReportAction.OnDismissDelete) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SessionReportContent(
    uiState: SessionReportUiState,
    aiCoachViewModel: AiCoachViewModel,
    onNavigateToAiCoach: (String?) -> Unit = {},
    onAction: (SessionReportAction) -> Unit
) {
    val aiCoachState by aiCoachViewModel.uiState.collectAsState()
    val isAiCoachVisible = aiCoachState.status != AiCoachStatus.UNSUPPORTED
    val data = uiState.data
    val df = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = {
                    Column {
                        Text(
                            data?.event?.let { df.format(Date(it.date)) } ?: "Session",
                            style = MaterialTheme.typography.titleLarge
                        )
                        data?.group?.name?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = AppTopBarSubtitleColor)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(SessionReportAction.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (data != null) {
                        IconButton(onClick = { onAction(SessionReportAction.OnRequestDelete) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Event")
                        }
                        if (uiState.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            IconButton(onClick = { onAction(SessionReportAction.OnExportCsv) }) {
                                Icon(Icons.Default.Download, contentDescription = "Export CSV")
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            AiFloatingActionButton(
                isVisible = isAiCoachVisible,
                onClick = {
                    val contextString = data?.let { d ->
                        "Session: ${d.event.name}\nTotal Athletes: ${d.totalAthletes}\n" +
                        "Tests:\n" + d.tests.joinToString("\n") { it.name }
                    }
                    onNavigateToAiCoach(contextString)
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> CenterSpinner()
            data == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Session not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> SessionReportBody(uiState = uiState, padding = padding, onAction = onAction)
        }
        if (uiState.isSwitcherOpen && data != null) {
            SessionSwitcherSheet(
                sessions = data.groupSessions,
                currentId = data.event.id,
                onPick = { onAction(SessionReportAction.OnSwitchSession(it.id)) },
                onDismiss = { onAction(SessionReportAction.OnDismissSwitcher) }
            )
        }
    }
}

@Composable
fun SessionReportBody(
    uiState: SessionReportUiState,
    padding: PaddingValues,
    onAction: (SessionReportAction) -> Unit
) {
    val data = uiState.data!!
    val activeTestId = uiState.selectedTestId ?: data.tests.firstOrNull()?.id
    val activeRows = activeTestId?.let { data.leaderboardByTest[it] }.orEmpty()
    val absent = activeTestId?.let { data.absentByTest[it] }.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { SessionPill(session = data.event, onTap = { onAction(SessionReportAction.OnOpenSwitcher) }) }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip("${data.tests.size} tests")
                MetaChip("${data.athletesTested}/${data.totalAthletes} tested")
                val flagged = activeRows.count { it.flagged } + absent.count { it.flagged }
                if (flagged > 0) MetaChip("$flagged flagged", danger = true)
            }
        }

        if (data.tests.isNotEmpty()) {
            item {
                if (data.tests.size > 1) {
                    val selectedIndex = data.tests.indexOfFirst { it.id == activeTestId }.coerceAtLeast(0)
                    ScrollableTabRow(
                        selectedTabIndex = selectedIndex,
                        edgePadding = 0.dp
                    ) {
                        data.tests.forEachIndexed { idx, t ->
                            Tab(
                                selected = idx == selectedIndex,
                                onClick = { onAction(SessionReportAction.OnSelectTest(t.id)) },
                                text = { Text(t.name, maxLines = 1, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                } else {
                    Text(
                        data.tests.first().name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // 1. Horizontal Bar Chart (Roster Comparison)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Roster Comparison", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        HorizontalBarChart(rows = activeRows)
                    }
                }
            }

            // 2. Stats Summary
            val validScores = activeRows.mapNotNull { it.rawScore }
            val maxVal = if (validScores.isNotEmpty()) validScores.maxOrNull() ?: 0.0 else 0.0
            val minVal = if (validScores.isNotEmpty()) validScores.minOrNull() ?: 0.0 else 0.0
            val avgVal = if (validScores.isNotEmpty()) validScores.average() else 0.0
            val unit = activeRows.firstOrNull()?.unit ?: ""

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Stats Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatSummaryItem("Max", maxVal, unit, modifier = Modifier.weight(1f))
                            StatSummaryItem("Avg", avgVal, unit, modifier = Modifier.weight(1f))
                            StatSummaryItem("Min", minVal, unit, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 3. Coaching Interpretation & Remediation Card
            val activeTest = data.tests.find { it.id == activeTestId }
            val interpretationText = when {
                activeTest == null -> "Select a test to view coaching tips."
                activeTest.name.contains("Jump", ignoreCase = true) || activeTest.categoryId.contains("power", ignoreCase = true) ->
                    "Vertical jump is a primary indicator of lower-body explosive power. Training should focus on plyometrics, Olympic lifting, and power cleans. Athletes in the Red zone need core strength and landing mechanics coaching."
                activeTest.name.contains("Sprint", ignoreCase = true) || activeTest.categoryId.contains("speed", ignoreCase = true) ->
                    "40m sprint tests acceleration and top-end speed. Focus on sprint mechanics, drive phase alignment, and hamstring strength. Red zone athletes require stride frequency and acceleration work."
                activeTest.name.contains("Squat", ignoreCase = true) || activeTest.categoryId.contains("strength", ignoreCase = true) ->
                    "1RM Squat is the baseline test for lower body absolute strength. Focus on progressive overload in squat variations. Red zone athletes need technique check and core stabilization before heavy lifting."
                activeTest.name.contains("Beep", ignoreCase = true) || activeTest.categoryId.contains("endurance", ignoreCase = true) || activeTest.name.contains("PACER", ignoreCase = true) ->
                    "Beep test measures aerobic capacity and fatigue resistance. Focus on high-intensity interval training (HIIT) and aerobic base building. Red zone athletes require low-intensity steady-state (LISS) conditioning."
                activeTest.name.contains("Agility", ignoreCase = true) || activeTest.categoryId.contains("agility", ignoreCase = true) ->
                    "Pro Agility (5-10-5) shuttle tests lateral acceleration, change of direction speed, and deceleration. Focus on center of gravity control and footwork. Red zone athletes should focus on deceleration mechanics and deceleration drills."
                else -> "Assessment benchmarks fitness standards. Performance is mapped to age-and-sex norms. Athletes in needs-improvement zones require foundational corrective training."
            }
            val redZoneAthletes = activeRows.filter { it.percentile != null && it.percentile < 30 }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Coaching Interpretation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = interpretationText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (redZoneAthletes.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = PerformanceRed.copy(alpha = 0.12f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, PerformanceRed, RoundedCornerShape(8.dp))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = PerformanceRedText
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "Remediation Required (<30%ile)",
                                            color = PerformanceRedText,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    val names = redZoneAthletes.joinToString(", ") { 
                                        val valStr = if (it.rawScore != null && it.rawScore % 1.0 == 0.0) it.rawScore.toInt().toString() else String.format("%.1f", it.rawScore)
                                        "${it.athleteName} ($valStr ${it.unit})" 
                                    }
                                    Text(
                                        text = "Coaches need to customize training for: $names. Focus on foundational corrective drills.",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Leaderboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }

            if (activeRows.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text("No results recorded for this test yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(activeRows, key = { it.individualId }) { row ->
                    AthleteLeaderRow(
                        row = row,
                        onClick = { onAction(SessionReportAction.OnNavigateToAthlete(row.individualId)) }
                    )
                }
            }

            // Absent subsection
            if (absent.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Absent (${absent.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(absent, key = { "absent-${it.individualId}" }) { row ->
                    AbsentAthleteRow(row = row, onClick = { onAction(SessionReportAction.OnResumeTesting) })
                }
            }

            // Group trend
            item {
                Spacer(Modifier.height(4.dp))
                Text("Group trend", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                val trend = activeTestId?.let { data.groupTrendByTest[it] }.orEmpty()
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).padding(16.dp), contentAlignment = Alignment.Center) {
                        if (trend.size < 2) {
                            Text("Need 2+ sessions for trend", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        } else {
                            TrendBars(points = trend)
                        }
                    }
                }
            }

            // Missing-data card
            val missingNames = activeTestId?.let { data.missingByTest[it] }.orEmpty()
            if (missingNames.isNotEmpty()) {
                item {
                    MissingDataCard(
                        names = missingNames,
                        onResume = { onAction(SessionReportAction.OnResumeTesting) }
                    )
                }
            }
        } else {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No tests in this session.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun MetaChip(text: String, danger: Boolean = false) {
    AssistChip(
        onClick = {},
        label = { Text(text, style = MaterialTheme.typography.labelSmall) },
        colors = if (danger)
            AssistChipDefaults.assistChipColors(containerColor = PerformanceRed, labelColor = PerformanceRedText)
        else AssistChipDefaults.assistChipColors()
    )
}

@Composable
fun AbsentAthleteRow(row: LeaderboardRow, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(row.athleteName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            ZoneChip(classification = Classification.NO_DATA, label = "Absent")
        }
    }
}

@Composable
fun MissingDataCard(names: List<String>, onResume: () -> Unit) {
    Card(
        onClick = onResume,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Missing data: ${names.size} athlete${if (names.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Resume testing",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(6.dp))
            names.forEach { n -> Text("• $n", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable
fun TrendBars(points: List<Pair<Long, Float>>) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        val max = points.maxOf { it.second }.coerceAtLeast(1f)
        points.forEach { (_, v) ->
            val frac = (v / max).coerceIn(0f, 1f)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height((110 * frac).dp.coerceAtLeast(2.dp))
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                )
                Spacer(Modifier.height(4.dp))
                Text("${v.toInt()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StatSummaryItem(
    label: String,
    value: Double,
    unit: String,
    modifier: Modifier = Modifier
) {
    val valueStr = if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text("$valueStr $unit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CenterSpinner() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
