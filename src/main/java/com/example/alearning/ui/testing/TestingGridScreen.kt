package com.example.alearning.ui.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.testing.TestResult
import com.example.alearning.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TestingGridScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAthleteReport: (String) -> Unit,
    onNavigateToLeaderboard: (String, String, String) -> Unit,
    onNavigateToGroupReport: (String, String) -> Unit,
    viewModel: TestingGridViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    TestingGridContent(
        uiState = uiState,
        eventId = viewModel.eventId,
        groupId = viewModel.groupId,
        onAction = { action ->
            when (action) {
                is TestingGridAction.OnNavigateBack -> onNavigateBack()
                is TestingGridAction.OnNavigateToAthleteReport -> onNavigateToAthleteReport(action.individualId)
                is TestingGridAction.OnNavigateToLeaderboard -> onNavigateToLeaderboard(action.eventId, action.groupId, action.mode)
                is TestingGridAction.OnNavigateToGroupReport -> onNavigateToGroupReport(action.eventId, action.groupId)
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestingGridContent(
    uiState: TestingGridUiState,
    eventId: String,
    groupId: String,
    onAction: (TestingGridAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.phase == TestingPhase.EVENT_DETAIL) "Testing Event"
                        else "Live Testing"
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(TestingGridAction.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.phase == TestingPhase.LIVE_ENTRY) {
                        IconButton(onClick = {
                            onAction(TestingGridAction.OnNavigateToLeaderboard(eventId, groupId, "event"))
                        }) {
                            Icon(Icons.Default.Leaderboard, contentDescription = "Leaderboard")
                        }
                        IconButton(onClick = {
                            onAction(TestingGridAction.OnNavigateToGroupReport(eventId, groupId))
                        }) {
                            Icon(Icons.Default.Stop, contentDescription = "End Testing Event")
                        }
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
            uiState.errorMessage != null && uiState.gridData == null -> {
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
                        TextButton(onClick = { onAction(TestingGridAction.OnDismissError) }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
            uiState.phase == TestingPhase.EVENT_DETAIL -> {
                EventDetailPhase(
                    uiState = uiState,
                    onAction = onAction,
                    padding = padding
                )
            }
            else -> {
                LiveEntryPhase(
                    uiState = uiState,
                    onAction = onAction,
                    padding = padding
                )
            }
        }
    }

    uiState.editingCell?.let { cell ->
        ScoreEntryDialog(
            athleteName = "${cell.athlete.firstName} ${cell.athlete.lastName}",
            testName = cell.test.name,
            unit = cell.test.unit,
            currentScore = cell.currentResult?.rawScore,
            onDismiss = { onAction(TestingGridAction.OnDismissEditing) },
            onSave = { score -> onAction(TestingGridAction.OnSaveScore(score)) }
        )
    }
}

@Composable
private fun EventDetailPhase(
    uiState: TestingGridUiState,
    onAction: (TestingGridAction) -> Unit,
    padding: PaddingValues
) {
    val gridData = uiState.gridData
    val event = uiState.event

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Event info card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    event?.name ?: "Testing Event",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                event?.date?.let { date ->
                    val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
                    Text(
                        dateFormat.format(Date(date)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Column {
                        Text("${gridData?.students?.size ?: 0}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Athletes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column {
                        Text("${gridData?.tests?.size ?: 0}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Text("Tests", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Tests list
        if (gridData != null && gridData.tests.isNotEmpty()) {
            Text("Tests in this event", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(gridData.tests) { test ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(test.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(
                                    "${test.unit} - ${if (test.isHigherBetter) "Higher is better" else "Lower is better"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Start Testing CTA
        Button(
            onClick = { onAction(TestingGridAction.OnStartTesting) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = gridData?.students?.isNotEmpty() == true && gridData.tests.isNotEmpty()
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Start Testing", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun LiveEntryPhase(
    uiState: TestingGridUiState,
    onAction: (TestingGridAction) -> Unit,
    padding: PaddingValues
) {
    val gridData = uiState.gridData
    if (gridData == null || gridData.tests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Text("No tests configured for this event")
        }
        return
    }

    val horizontalScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .padding(8.dp)
            ) {
                Text(
                    "Athlete",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                gridData.tests.forEach { test ->
                    Box(
                        modifier = Modifier
                            .width(90.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            test.name.take(12),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(gridData.students) { athlete ->
                AthleteRow(
                    athlete = athlete,
                    tests = gridData.tests,
                    results = gridData.results,
                    horizontalScroll = horizontalScroll,
                    onCellClick = { test ->
                        onAction(TestingGridAction.OnStartEditing(athlete, test))
                    },
                    onAthleteClick = {
                        onAction(TestingGridAction.OnNavigateToAthleteReport(athlete.id))
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun AthleteRow(
    athlete: Individual,
    tests: List<FitnessTest>,
    results: List<TestResult>,
    horizontalScroll: androidx.compose.foundation.ScrollState,
    onCellClick: (FitnessTest) -> Unit,
    onAthleteClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAthleteClick
                )
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (athlete.isRestricted || athlete.medicalAlert != null) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Medical alert",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    "${athlete.lastName}, ${athlete.firstName.first()}.",
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (athlete.isRestricted) MaterialTheme.colorScheme.error
                            else Color.Unspecified
                )
            }
        }

        Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
            tests.forEach { test ->
                val result = results.find {
                    it.individualId == athlete.id && it.testId == test.id
                }
                ScoreCell(
                    result = result,
                    onClick = { onCellClick(test) }
                )
            }
        }
    }
}

@Composable
private fun ScoreCell(
    result: TestResult?,
    onClick: () -> Unit
) {
    val (bgColor, textColor, borderColor) = when {
        result == null -> Triple(Color.Transparent, Color.Unspecified, MaterialTheme.colorScheme.outlineVariant)
        result.percentile == null -> Triple(PerformanceGrey, PerformanceGreyText, PerformanceGreyBorder)
        result.percentile >= 60 -> Triple(PerformanceGreen, PerformanceGreenText, PerformanceGreenBorder)
        result.percentile >= 30 -> Triple(PerformanceYellow, PerformanceYellowText, PerformanceYellowBorder)
        else -> Triple(PerformanceRed, PerformanceRedText, PerformanceRedBorder)
    }

    Box(
        modifier = Modifier
            .width(90.dp)
            .height(44.dp)
            .border(0.5.dp, borderColor)
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (result != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    String.format("%.1f", result.rawScore),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor
                )
                result.percentile?.let { p ->
                    Text(
                        "${p}%",
                        fontSize = 10.sp,
                        color = textColor.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            Text("--", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ScoreEntryDialog(
    athleteName: String,
    testName: String,
    unit: String,
    currentScore: Double?,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var scoreText by remember { mutableStateOf(currentScore?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Score") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(athleteName, fontWeight = FontWeight.Bold)
                Text(testName, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { scoreText = it },
                    label = { Text("Score ($unit)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scoreText.toDoubleOrNull()?.let { onSave(it) }
                },
                enabled = scoreText.toDoubleOrNull() != null
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
