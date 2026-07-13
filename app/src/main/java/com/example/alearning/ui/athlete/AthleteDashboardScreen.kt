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
import com.example.alearning.ui.report.components.MiniSparkline
import com.example.alearning.ui.report.components.PercentileChip
import com.example.alearning.ui.report.components.ZoneChip
import com.example.alearning.ui.report.components.zoneColors
import com.example.alearning.ui.report.components.zoneLabel
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AthleteDashboardScreen(
    athleteId: String,
    contextSessionId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToTest: (String, String, String?) -> Unit,
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

    AthleteDashboardContent(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                AthleteDashboardAction.OnNavigateBack -> onNavigateBack()
                is AthleteDashboardAction.OnNavigateToTest ->
                    onNavigateToTest(viewModel.athleteId, action.testId, viewModel.contextSessionId)
                is AthleteDashboardAction.OnStartQuickTest ->
                    onStartQuickTest(viewModel.athleteId, action.testIds)
                else -> viewModel.onAction(action)
            }
        },
        aiCoachViewModel = aiCoachViewModel,
        onNavigateToAiCoach = onNavigateToAiCoach
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
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                data == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Athlete not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> AthleteBody(uiState = uiState, padding = PaddingValues(0.dp), onAction = onAction)
            }

            com.example.alearning.ui.aicoach.components.DraggableAiFab(
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
    }
}

@Composable
fun AthleteBody(
    uiState: AthleteDashboardUiState,
    padding: PaddingValues,
    onAction: (AthleteDashboardAction) -> Unit,
    headerContent: @Composable (() -> Unit)? = null
) {
    val data = uiState.data!!
    var perTestExpanded by rememberSaveable { mutableStateOf(false) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (headerContent != null) {
            item { headerContent() }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AthleteAlertCard(data.athlete)
                PremiumAthleteMetaCard(athleteData = data)
                LatestSessionCard(
                    date = data.contextSession?.date,
                    name = data.contextSession?.name,
                    testCount = data.sessionTestCount,
                    avgPercentile = data.athleteSessionAvgPctile
                )
                CategoryRadarCard(
                    radarData = uiState.radarData,
                    hasResults = data.tiles.isNotEmpty()
                )
            }
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
                        perTestExpanded = !perTestExpanded
                        val targetIndex = if (headerContent != null) 2 else 1
                        coroutineScope.launch {
                            if (perTestExpanded) {
                                listState.animateScrollToItem(targetIndex)
                            } else {
                                listState.animateScrollToItem(0)
                            }
                        }
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    if (date != null) "Latest session · ${df.format(Date(date))}" else "Latest session",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.fg.copy(alpha = 0.8f)
                )
                Text("$testCount test${if (testCount == 1) "" else "s"}${name?.let { " · $it" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = colors.fg)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(avgPercentile?.toString() ?: "—", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = colors.fg)
                    Text("avg %ile", style = MaterialTheme.typography.labelSmall, color = colors.fg.copy(alpha = 0.8f))
                }
                ZoneChip(classification = cls)
            }
        }
    }
}

@Composable
fun TestTile(tile: AthleteTestTile, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(150.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().clickable(onClick = onClick).padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(tile.test.name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tile.latestResult?.let {
                        val s = if (it.rawScore % 1.0 == 0.0) it.rawScore.toInt().toString() else String.format("%.1f", it.rawScore)
                        "$s ${tile.test.unit}"
                    } ?: "—",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val tileLabel = tile.latestResult?.classification?.takeIf { it.isNotBlank() }
                    ?: zoneLabel(tile.classification)
                ZoneChip(classification = tile.classification, label = tileLabel)
                Spacer(Modifier.weight(1f))
                MiniSparkline(points = tile.sparkline)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                PercentileChip(percentile = tile.latestResult?.percentile)
                Text("${tile.rawSparkline.size} try${if (tile.rawSparkline.size == 1) "" else "s"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FlagListRow(flag: AthleteFlag, onClick: () -> Unit) {
    val isActionable = flag.testId != null || flag.testIds.isNotEmpty() || flag.type == FlagType.MISSING_DATA
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SportOrangeContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().then(if (isActionable) Modifier.clickable(onClick = onClick) else Modifier).padding(12.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Skill Matrix",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            val hasAxes = radarData != null && radarData.axisScores.any { it.normalizedScore > 0f }
            if (hasAxes) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    RadarChart(
                        data = radarData!!,
                        modifier = Modifier.height(280.dp),
                        color = SportOrange
                    )
                }
                Text(
                    "Average percentile per category. A larger area indicates a stronger overall athletic profile.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (hasResults) "Not enough percentile data to chart yet."
                        else "Record results to see a category profile.",
                        style = MaterialTheme.typography.bodyMedium,
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
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(athlete.fullName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${athleteData.groups.firstOrNull()?.name ?: "No Group"} · Age ${athlete.currentAge}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            athleteData.athleteSessionAvgPctile?.let { avg ->
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                    CircularProgressIndicator(
                        progress = { avg / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = SportOrange,
                        trackColor = Color.White.copy(alpha = 0.1f),
                        strokeWidth = 6.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "$avg",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text("%ile", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun PerTestHeader(testCount: Int, expanded: Boolean, onToggle: () -> Unit) {
    val backgroundColor = if (expanded) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (expanded) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Individual Test Breakdown", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = contentColor)
                Text(
                    "$testCount test${if (testCount == 1) "" else "s"} · ${if (expanded) "tap to collapse" else "tap to view trends"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (expanded) contentColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = if (expanded) contentColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
