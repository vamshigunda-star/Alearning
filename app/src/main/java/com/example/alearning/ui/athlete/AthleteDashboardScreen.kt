package com.example.alearning.ui.athlete

import android.util.Log
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.reports.AthleteFlag
import com.example.alearning.domain.usecase.testing.AthleteRadarData
import com.example.alearning.domain.model.reports.AthleteTestTile
import com.example.alearning.reports.components.MiniSparkline
import com.example.alearning.reports.components.PercentileChip
import com.example.alearning.reports.components.ZoneChip
import com.example.alearning.reports.components.zoneColors
import com.example.alearning.reports.components.zoneLabel
import com.example.alearning.domain.model.reports.Classification
import com.example.alearning.domain.model.reports.FlagType
import com.example.alearning.domain.model.reports.AthleteDashboardData
import com.example.alearning.ui.components.AppTopBar
import com.example.alearning.ui.components.AppTopBarSubtitleColor
import com.example.alearning.ui.components.charts.RadarChart
import com.example.alearning.ui.theme.NavyPrimary
import com.example.alearning.ui.theme.PerformanceRed
import com.example.alearning.ui.theme.PerformanceRedText
import com.example.alearning.ui.theme.SportOrange
import com.example.alearning.ui.theme.SportOrangeContainer
import com.example.alearning.ui.theme.SportOrangeVariant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.alearning.util.CsvExporter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.Download

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.activity.compose.BackHandler

import com.example.alearning.ui.aicoach.AiCoachViewModel
import com.example.alearning.domain.repository.AiCoachStatus
import com.example.alearning.ui.aicoach.components.AiFloatingActionButton

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AthleteDashboardScreen(
    athleteId: String,
    contextSessionId: String? = null,
    onNavigateBack: () -> Unit,
    onStartQuickTest: (String, List<String>) -> Unit, // (athleteId, testIds)
    onNavigateToAiCoach: (String?) -> Unit,
    viewModel: AthleteDashboardViewModel = hiltViewModel(),
    aiCoachViewModel: AiCoachViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val navigator = rememberListDetailPaneScaffoldNavigator<Any>()

    LaunchedEffect(athleteId, contextSessionId) {
        viewModel.loadDashboard(athleteId, contextSessionId)
    }

    LaunchedEffect(viewModel.exportEvent) {
        viewModel.exportEvent.collect { request ->
            when (request) {
                is AthleteDashboardViewModel.ExportRequest.Athlete -> {
                    CsvExporter.exportAthleteResults(context, request.athlete, request.results, request.tests)
                }
            }
        }
    }

    BackHandler(navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                AthleteDashboardContent(
                    uiState = uiState,
                    onAction = { action ->
                        when (action) {
                            AthleteDashboardAction.OnNavigateBack -> {
                                if (navigator.canNavigateBack()) {
                                    navigator.navigateBack()
                                } else {
                                    onNavigateBack()
                                }
                            }
                            is AthleteDashboardAction.OnNavigateToTest ->
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, action.testId as Any)
                            is AthleteDashboardAction.OnStartQuickTest ->
                                onStartQuickTest(viewModel.athleteId, action.testIds)
                            else -> viewModel.onAction(action)
                        }
                    },
                    aiCoachViewModel = aiCoachViewModel,
                    onNavigateToAiCoach = onNavigateToAiCoach
                )
            }
        },
        detailPane = {
            AnimatedPane {
                val testId = navigator.currentDestination?.content as? String
                if (testId != null) {
                    AthleteTestDetailScreen(
                        athleteId = viewModel.athleteId,
                        testId = testId,
                        contextSessionId = viewModel.contextSessionId,
                        onNavigateBack = { navigator.navigateBack() }
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select a test to view details", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteDashboardContent(
    uiState: AthleteDashboardUiState,
    aiCoachViewModel: AiCoachViewModel,
    onNavigateToAiCoach: (String?) -> Unit = {},
    onAction: (AthleteDashboardAction) -> Unit
) {
    val aiCoachState by aiCoachViewModel.uiState.collectAsState()
    val isAiCoachVisible = aiCoachState.status != AiCoachStatus.UNSUPPORTED
    val data = uiState.data
    Scaffold(
        topBar = {
            AppTopBar(
                title = {
                    Column {
                        Text(data?.athlete?.fullName ?: "Athlete", style = MaterialTheme.typography.titleLarge)
                        data?.athlete?.let { ind ->
                            val grp = data.groups.firstOrNull()?.name?.let { " · $it" } ?: ""
                            Text(
                                "${ind.currentAge}y · ${ind.sex.name.lowercase().replaceFirstChar { it.uppercase() }}$grp",
                                style = MaterialTheme.typography.labelSmall, color = AppTopBarSubtitleColor
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(AthleteDashboardAction.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (data != null) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            IconButton(onClick = { onAction(AthleteDashboardAction.OnExportCsv) }) {
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
                        "Athlete: ${d.athlete.fullName}\nAge: ${d.athlete.currentAge}\nAvg Percentile: ${d.athleteSessionAvgPctile}\nTest Results:\n" +
                        d.tiles.joinToString("\n") { t -> "${t.test.name}: ${t.latestResult?.rawScore} ${t.test.unit} (${t.latestResult?.percentile}th percentile)" }
                    }
                    onNavigateToAiCoach(contextString)
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            data == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Athlete not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> AthleteBody(uiState = uiState, padding = padding, onAction = onAction)
        }
    }
}

@Composable
fun AthleteBody(
    uiState: AthleteDashboardUiState,
    padding: PaddingValues,
    onAction: (AthleteDashboardAction) -> Unit
) {
    val data = uiState.data!!
    var perTestExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Log.d("AthleteDashboardScreen", "Rendering AthleteBody for ${data.athlete.fullName}, tiles=${data.tiles.size}, expanded=$perTestExpanded")
            AthleteAlertCard(data.athlete)
        }

        item {
            PremiumAthleteMetaCard(athleteData = data)
        }

        item {
            LatestSessionCard(
                date = data.contextSession?.date,
                name = data.contextSession?.name,
                testCount = data.sessionTestCount,
                avgPercentile = data.athleteSessionAvgPctile
            )
        }

        item {
            CategoryRadarCard(
                radarData = uiState.radarData,
                hasResults = data.tiles.isNotEmpty()
            )
        }

        if (data.tiles.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No test results yet for ${data.athlete.fullName}.", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("Add results from a session to populate this view.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            item {
                PerTestHeader(
                    testCount = data.tiles.size,
                    expanded = perTestExpanded,
                    onToggle = { 
                        Log.d("AthleteDashboardScreen", "Toggling per-test section. Current state: $perTestExpanded")
                        perTestExpanded = !perTestExpanded 
                    }
                )
            }
            if (perTestExpanded) {
                items(data.tiles.chunked(2), key = { row -> row.joinToString("|") { it.test.id } }) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { tile ->
                            Box(modifier = Modifier.weight(1f)) {
                                TestTile(tile = tile, onClick = { onAction(AthleteDashboardAction.OnNavigateToTest(tile.test.id)) })
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        if (data.flags.isNotEmpty()) {
            item { Text("Flags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(data.flags, key = { "${it.type}-${it.message}" }) { f ->
                FlagListRow(
                    flag = f,
                    onClick = {
                        when {
                            f.testIds.isNotEmpty() ->
                                onAction(AthleteDashboardAction.OnStartQuickTest(f.testIds))
                            f.testId != null ->
                                onAction(AthleteDashboardAction.OnStartQuickTest(listOf(f.testId)))
                            f.type == FlagType.MISSING_DATA && data.outstandingTests.isNotEmpty() ->
                                onAction(AthleteDashboardAction.OnStartQuickTest(data.outstandingTests.map { it.id }))
                        }
                    }
                )
            }
        }

        if (data.outstandingTests.isNotEmpty()) {
            item {
                OutstandingTestsCard(
                    tests = data.outstandingTests,
                    onTestClick = { onAction(AthleteDashboardAction.OnStartQuickTest(listOf(it))) }
                )
            }
        }
    }
}

@Composable
fun AthleteAlertCard(athlete: Individual) {
    if (athlete.medicalAlert == null && !athlete.isRestricted) return
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PerformanceRed)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = PerformanceRedText)
            Spacer(Modifier.width(8.dp))
            Column {
                if (athlete.isRestricted) Text("Restricted", color = PerformanceRedText, fontWeight = FontWeight.Bold)
                athlete.medicalAlert?.let { Text(it, color = PerformanceRedText, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}

@Composable
fun LatestSessionCard(date: Long?, name: String?, testCount: Int, avgPercentile: Int?) {
    val df = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val cls = when {
        avgPercentile == null -> Classification.NO_DATA
        avgPercentile >= 60 -> Classification.SUPERIOR
        avgPercentile >= 30 -> Classification.HEALTHY
        else -> Classification.NEEDS_IMPROVEMENT
    }
    val colors = zoneColors(cls)
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.bg)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (date != null) "Latest session · ${df.format(Date(date))}" else "Latest session",
                style = MaterialTheme.typography.labelMedium,
                color = colors.fg
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(avgPercentile?.toString() ?: "—", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = colors.fg)
                Column {
                    Text("avg percentile", style = MaterialTheme.typography.labelSmall, color = colors.fg)
                    Text("$testCount test${if (testCount == 1) "" else "s"}${name?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.labelSmall, color = colors.fg)
                }
            }
            ZoneChip(classification = cls)
        }
    }
}

@Composable
fun TestTile(tile: AthleteTestTile, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(140.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(tile.test.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Text(
                    text = tile.latestResult?.let {
                        val s = if (it.rawScore % 1.0 == 0.0) it.rawScore.toInt().toString() else String.format("%.1f", it.rawScore)
                        "$s ${tile.test.unit}"
                    } ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // Prefer the snapshot's data-driven label (e.g., "Excellent", "Healthy Fitness
                // Zone") over the synthetic zone label so the rich norm-CSV text is shown.
                val tileLabel = tile.latestResult?.classification?.takeIf { it.isNotBlank() }
                    ?: zoneLabel(tile.classification)
                ZoneChip(classification = tile.classification, label = tileLabel)
                Spacer(Modifier.weight(1f))
                MiniSparkline(points = tile.sparkline)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PercentileChip(percentile = tile.latestResult?.percentile)
                Text("${tile.rawSparkline.size} attempt${if (tile.rawSparkline.size == 1) "" else "s"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FlagListRow(flag: AthleteFlag, onClick: () -> Unit) {
    val isActionable = flag.testId != null || flag.testIds.isNotEmpty() || flag.type == FlagType.MISSING_DATA
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SportOrangeContainer),
        enabled = isActionable
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = SportOrangeVariant)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(flag.type.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }, fontWeight = FontWeight.SemiBold)
                Text(flag.message, style = MaterialTheme.typography.bodySmall, color = SportOrangeVariant)
                if (flag.type == FlagType.MISSING_DATA && flag.testNames.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    flag.testNames.forEach { name ->
                        Text("• $name", style = MaterialTheme.typography.labelSmall, color = SportOrangeVariant)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Tap to complete in Quick Test",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SportOrangeVariant
                    )
                }
            }
            if (isActionable) {
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = SportOrangeVariant)
            }
        }
    }
}

@Composable
fun OutstandingTestsCard(tests: List<FitnessTest>, onTestClick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                Text("Outstanding tests (${tests.size})", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(if (expanded) "Hide" else "Show", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tests.forEach { test ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTestClick(test.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("• ${test.name}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            Text("Start", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryRadarCard(radarData: AthleteRadarData?, hasResults: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Performance Across Multiple Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            val hasAxes = radarData != null && radarData.axisScores.isNotEmpty()
            if (hasAxes) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    RadarChart(
                        data = radarData!!,
                        modifier = Modifier.height(300.dp),
                        color = SportOrange
                    )
                }
                Text(
                    "Average percentile per fitness category. Larger area = stronger overall profile.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (hasResults) "Not enough percentile data to chart yet."
                        else "Record results to see a category profile.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumAthleteMetaCard(athleteData: AthleteDashboardData) {
    val athlete = athleteData.athlete
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(SportOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = athlete.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(athlete.fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    "${athleteData.groups.firstOrNull()?.name ?: "—"} · Age ${athlete.currentAge}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            athleteData.athleteSessionAvgPctile?.let { avg ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$avg",
                        style = MaterialTheme.typography.headlineMedium,
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
fun PerTestHeader(testCount: Int, expanded: Boolean, onToggle: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Per-test results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "$testCount test${if (testCount == 1) "" else "s"} · ${if (expanded) "tap to collapse" else "tap to view trends"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
