package com.example.alearning.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.reports.RecentSessionRow
import com.example.alearning.domain.repository.AiCoachStatus
import com.example.alearning.ui.report.components.SessionSwitcherSheet
import com.example.alearning.ui.aicoach.AiCoachViewModel
import com.example.alearning.ui.aicoach.components.AiFloatingActionButton
import com.example.alearning.ui.athlete.AthleteBody
import com.example.alearning.ui.athlete.AthleteDashboardUiState
import com.example.alearning.ui.components.AppTopBar
import com.example.alearning.ui.components.AppTopBarSubtitleColor
import com.example.alearning.ui.session.SessionReportBody
import com.example.alearning.ui.session.SessionReportUiState
import com.example.alearning.ui.theme.SportOrange
import com.example.alearning.util.CsvExporter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(
    onNavigateToGroup: (String) -> Unit,
    onNavigateToSession: (String, String) -> Unit,
    onNavigateToAthlete: (String) -> Unit,
    onNavigateToTest: (String, String) -> Unit = { _, _ -> },
    onNavigateToAiCoach: (String?) -> Unit = {},
    onStartQuickTest: (String, List<String>) -> Unit = { _, _ -> },
    viewModel: ReportsHubViewModel = hiltViewModel(),
    aiCoachViewModel: AiCoachViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val aiCoachState by aiCoachViewModel.uiState.collectAsState()
    val isAiCoachVisible = aiCoachState.status != AiCoachStatus.UNSUPPORTED
    
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    LaunchedEffect(viewModel.exportEvent) {
        viewModel.exportEvent.collect { request ->
            when (request) {
                is ReportsHubViewModel.ExportRequest.Athlete -> {
                    CsvExporter.exportAthleteResults(context, request.athlete, request.results, request.tests)
                }
                is ReportsHubViewModel.ExportRequest.Event -> {
                    CsvExporter.exportEventResults(context, request.eventName, request.results, request.tests)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = {
                    Column {
                        Text("Reports", style = MaterialTheme.typography.titleLarge)
                        Text("Performance insights", style = MaterialTheme.typography.labelSmall, color = AppTopBarSubtitleColor)
                    }
                },
                actions = {
                    if (selectedTab == 0 && uiState.athleteData != null) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            IconButton(onClick = { viewModel.onAction(ReportsHubAction.ExportAthleteCsv) }) {
                                Icon(Icons.Default.Download, contentDescription = "Export Athlete CSV")
                            }
                        }
                    } else if (selectedTab == 1 && uiState.eventData != null) {
                        IconButton(onClick = { viewModel.onAction(ReportsHubAction.RequestDeleteEvent) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Event")
                        }
                        if (uiState.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            IconButton(onClick = { viewModel.onAction(ReportsHubAction.ExportEventCsv) }) {
                                Icon(Icons.Default.Download, contentDescription = "Export Event CSV")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            ReportsHubContent(
                uiState = uiState,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onAction = { action -> 
                    when (action) {
                        is ReportsHubAction.OnStartQuickTest -> onStartQuickTest(action.athleteId, action.testIds)
                        is ReportsHubAction.OnNavigateToAthleteDashboard -> onNavigateToAthlete(action.athleteId)
                        else -> viewModel.onAction(action)
                    }
                },
                onNavigateToTest = onNavigateToTest,
                modifier = Modifier.fillMaxSize()
            )
            
            com.example.alearning.ui.aicoach.components.DraggableAiFab(
                isVisible = isAiCoachVisible,
                onClick = {
                    val contextString = if (selectedTab == 0) {
                        uiState.athleteData?.let { d ->
                            "Athlete: ${d.athlete.fullName}\nAge: ${d.athlete.currentAge}\nAvg Percentile: ${d.athleteSessionAvgPctile}\nTest Results:\n" +
                            d.tiles.joinToString("\n") { t -> "${t.test.name}: ${t.latestResult?.rawScore} ${t.test.unit} (${t.latestResult?.percentile}th percentile)" }
                        }
                    } else {
                        uiState.eventData?.let { d ->
                            "Session: ${d.event.name}\nTotal Athletes: ${d.totalAthletes}\n" +
                            "Tests:\n" + d.tests.joinToString("\n") { it.name }
                        }
                    }
                    onNavigateToAiCoach(contextString)
                }
            )
        }

        if (uiState.showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.onAction(ReportsHubAction.DismissDeleteEvent) },
                title = { Text("Delete Event?") },
                text = { Text("Are you sure you want to permanently delete this event and all associated test results? This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = { viewModel.onAction(ReportsHubAction.ConfirmDeleteEvent) }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.onAction(ReportsHubAction.DismissDeleteEvent) }) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        if (uiState.isSwitcherOpen && uiState.eventData != null) {
            SessionSwitcherSheet(
                sessions = uiState.eventData!!.groupSessions,
                currentId = uiState.eventData!!.event.id,
                onPick = { viewModel.onAction(ReportsHubAction.OnSwitchSession(it.id)) },
                onDismiss = { viewModel.onAction(ReportsHubAction.OnDismissSwitcher) }
            )
        }
    }
}

@Composable
private fun ReportsHubContent(
    uiState: ReportsHubUiState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onAction: (ReportsHubAction) -> Unit,
    onNavigateToTest: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabTitles = listOf("Athlete Profile", "Event Report")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> AthleteProfileTab(uiState = uiState, onAction = onAction, onNavigateToTest = onNavigateToTest)
            1 -> EventReportTab(uiState = uiState, onAction = onAction)
        }
    }
}

@Composable
private fun AthleteProfileTab(
    uiState: ReportsHubUiState,
    onAction: (ReportsHubAction) -> Unit,
    onNavigateToTest: (String, String) -> Unit
) {
    val data = uiState.homeData

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

    val allAthletes = data.allAthletes

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.selectedAthleteId == null) {
            Box(modifier = Modifier.padding(16.dp)) {
                if (allAthletes.isNotEmpty()) {
                    AthletePickerRow(
                        athletes = allAthletes,
                        selectedId = uiState.selectedAthleteId,
                        onSelect = { onAction(ReportsHubAction.SelectAthlete(it)) }
                    )
                } else {
                    Text(
                        "Select an athlete from the Roster tab to view their profile.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (uiState.isLoadingAthlete) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.selectedAthleteId != null) {
            val athleteState = AthleteDashboardUiState(
                data = uiState.athleteData,
                radarData = uiState.athleteRadarData,
                isLoading = false,
                errorMessage = uiState.errorMessage,
                isExporting = uiState.isExporting
            )
            AthleteBody(
                uiState = athleteState,
                padding = PaddingValues(0.dp),
                onAction = { action ->
                    when (action) {
                        is com.example.alearning.ui.athlete.AthleteDashboardAction.OnStartQuickTest -> {
                            uiState.selectedAthleteId?.let { athleteId ->
                                onAction(ReportsHubAction.OnStartQuickTest(athleteId, action.testIds))
                            }
                        }
                        is com.example.alearning.ui.athlete.AthleteDashboardAction.OnNavigateToTest -> {
                            uiState.selectedAthleteId?.let { athleteId ->
                                onNavigateToTest(athleteId, action.testId)
                            }
                        }
                        else -> {}
                    }
                },
                headerContent = {
                    AthletePickerRow(
                        athletes = allAthletes,
                        selectedId = uiState.selectedAthleteId,
                        onSelect = { onAction(ReportsHubAction.SelectAthlete(it)) }
                    )
                }
            )
        } else {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Tap an athlete above to view their profile.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EventReportTab(
    uiState: ReportsHubUiState,
    onAction: (ReportsHubAction) -> Unit
) {
    val data = uiState.homeData

    if (uiState.isLoadingHome) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    // Only show sessions that are associated with a group, as Event Report requires a leaderboard
    val sessions = data?.recentSessions?.filter { it.groupId != null } ?: emptyList()
    if (sessions.isEmpty()) {
        EmptyState(
            icon = "📋",
            title = "No Testing Events",
            subtitle = "Create a testing event from the Home screen, record results, and they'll appear here for analysis."
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.selectedEventId == null) {
            Box(modifier = Modifier.padding(16.dp)) {
                EventPickerRow(
                    sessions = sessions,
                    selectedEventId = uiState.selectedEventId,
                    onSelect = { row ->
                        val gid = row.groupId ?: return@EventPickerRow
                        onAction(ReportsHubAction.SelectEvent(row.event.id, gid))
                    }
                )
            }
        }

        if (uiState.isLoadingEvent) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.selectedEventId != null) {
            val sessionState = SessionReportUiState(
                data = uiState.eventData,
                selectedTestId = uiState.selectedEventTestId,
                isSwitcherOpen = uiState.isSwitcherOpen,
                isLoading = false,
                isExporting = uiState.isExporting,
                errorMessage = uiState.errorMessage,
                showDeleteDialog = uiState.showDeleteDialog,
                isDeleted = uiState.isEventDeleted
            )
            SessionReportBody(
                uiState = sessionState,
                padding = PaddingValues(0.dp),
                onAction = { sessionAction ->
                    when (sessionAction) {
                        is com.example.alearning.ui.session.SessionReportAction.OnSelectTest -> onAction(ReportsHubAction.SelectEventTest(sessionAction.testId))
                        is com.example.alearning.ui.session.SessionReportAction.OnOpenSwitcher -> onAction(ReportsHubAction.OnOpenSwitcher)
                        else -> {} // ignore other actions
                    }
                },
                headerContent = {
                    EventPickerRow(
                        sessions = sessions,
                        selectedEventId = uiState.selectedEventId,
                        onSelect = { row ->
                            val gid = row.groupId ?: return@EventPickerRow
                            onAction(ReportsHubAction.SelectEvent(row.event.id, gid))
                        }
                    )
                }
            )
        } else {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("Select an event above to view the report.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AthletePickerRow(
    athletes: List<Pair<String, String>>,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }
    val selectedName = athletes.firstOrNull { it.first == selectedId }?.second ?: "Select Athlete"

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { showSheet = true },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Athlete Profile", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(selectedName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Change")
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Text(
                    "Select Athlete",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                LazyColumn {
                    items(athletes.size) { index ->
                        val (id, name) = athletes[index]
                        ListItem(
                            headlineContent = { Text(name, fontWeight = if (id == selectedId) FontWeight.Bold else FontWeight.Normal) },
                            modifier = Modifier.clickable {
                                onSelect(id)
                                showSheet = false
                            },
                            trailingContent = {
                                if (id == selectedId) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = SportOrange)
                                }
                            }
                        )
                    }
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
    var showSheet by remember { mutableStateOf(false) }
    val selectedRow = sessions.firstOrNull { it.event.id == selectedEventId }
    val displayText = selectedRow?.event?.name ?: "Select Testing Event"
    val subtitleText = selectedRow?.let { "${it.groupName ?: ""} · ${df.format(Date(it.event.date))}" } ?: "View session report"

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { showSheet = true },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(displayText, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(subtitleText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Change")
        }
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Text(
                    "Select Testing Event",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
                LazyColumn {
                    items(sessions.size) { index ->
                        val row = sessions[index]
                        val isSelected = row.event.id == selectedEventId
                        ListItem(
                            headlineContent = { Text(row.event.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            supportingContent = { Text("${row.groupName ?: ""} · ${df.format(Date(row.event.date))}") },
                            modifier = Modifier.clickable {
                                onSelect(row)
                                showSheet = false
                            },
                            trailingContent = {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = SportOrange)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(icon: String, title: String, subtitle: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, style = MaterialTheme.typography.displayMedium)
            }
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
