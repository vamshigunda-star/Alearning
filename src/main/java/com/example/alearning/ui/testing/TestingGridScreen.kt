package com.example.alearning.ui.testing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.standards.TimingMode
import com.example.alearning.domain.model.testing.TestResult
import com.example.alearning.ui.theme.*
import com.example.alearning.ui.components.testing.TestInputSwitcher
import kotlinx.coroutines.delay
import java.util.Locale

private val PendingTint = Color(0xFFFFF3CD)
private val PendingBorder = Color(0xFFE65100)

@Composable
fun TestingGridScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAthleteReport: (String) -> Unit,
    onNavigateToLeaderboard: (String, String, String) -> Unit,
    onNavigateToGroupReport: (String, String) -> Unit,
    onNavigateToStopwatch: (String, String, String, String?) -> Unit = { _, _, _, _ -> },
    viewModel: TestingGridViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Intercept system back so unsaved drafts trigger the discard dialog before leaving.
    BackHandler(enabled = uiState.hasPendingChanges && uiState.phase == TestingPhase.LIVE_ENTRY) {
        viewModel.onAction(TestingGridAction.OnRequestBack)
    }

    TestingGridContent(
        uiState = uiState,
        eventId = viewModel.eventId,
        groupId = viewModel.groupId,
        onAction = { action ->
            when (action) {
                is TestingGridAction.OnNavigateBack -> {
                    if (uiState.hasPendingChanges && uiState.phase == TestingPhase.LIVE_ENTRY) {
                        viewModel.onAction(TestingGridAction.OnRequestBack)
                    } else {
                        onNavigateBack()
                    }
                }
                is TestingGridAction.OnNavigateToAthleteReport -> onNavigateToAthleteReport(action.individualId)
                is TestingGridAction.OnNavigateToLeaderboard -> onNavigateToLeaderboard(action.eventId, action.groupId, action.mode)
                is TestingGridAction.OnNavigateToGroupReport -> onNavigateToGroupReport(action.eventId, action.groupId)
                is TestingGridAction.OnNavigateToStopwatch -> onNavigateToStopwatch(action.eventId, action.fitnessTestId, action.groupId, action.individualId)
                else -> viewModel.onAction(action)
            }
        }
    )

    if (uiState.showDiscardDialog) {
        DiscardDraftsDialog(
            count = uiState.pendingResults.size,
            onConfirm = {
                viewModel.onAction(TestingGridAction.OnConfirmDiscard)
                onNavigateBack()
            },
            onDismiss = { viewModel.onAction(TestingGridAction.OnDismissDiscard) }
        )
    }

    uiState.deleteCandidate?.let { candidate ->
        DeleteResultDialog(
            athleteName = "${candidate.athlete.firstName} ${candidate.athlete.lastName}",
            testName = candidate.test.name,
            onConfirm = { viewModel.onAction(TestingGridAction.OnConfirmDelete) },
            onDismiss = { viewModel.onAction(TestingGridAction.OnDismissDelete) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestingGridContent(
    uiState: TestingGridUiState,
    eventId: String,
    groupId: String,
    onAction: (TestingGridAction) -> Unit
) {
    var sessionSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(uiState.phase) {
        if (uiState.phase == TestingPhase.LIVE_ENTRY) {
            while (true) {
                delay(1000L)
                sessionSeconds++
            }
        }
    }

    val sessionTimeStr = remember(sessionSeconds) {
        "%d:%02d".format(sessionSeconds / 60, sessionSeconds % 60)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    when (uiState.phase) {
                        TestingPhase.LIVE_ENTRY -> Column {
                            Text("Live Testing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(sessionTimeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TestingPhase.EVENT_DETAIL -> Text("Testing Event", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(TestingGridAction.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.phase == TestingPhase.LIVE_ENTRY) {
                        IconButton(onClick = { onAction(TestingGridAction.OnNavigateToLeaderboard(eventId, groupId, "event")) }) {
                            Icon(Icons.Default.Leaderboard, contentDescription = "Leaderboard")
                        }
                        IconButton(onClick = { onAction(TestingGridAction.OnNavigateToGroupReport(eventId, groupId)) }) {
                            Icon(Icons.Default.Assessment, contentDescription = "Session Report")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (uiState.phase == TestingPhase.LIVE_ENTRY && uiState.hasPendingChanges) {
                SubmitBar(
                    pendingCount = uiState.pendingResults.size,
                    isSubmitting = uiState.isSubmitting,
                    onSubmit = { onAction(TestingGridAction.OnSubmitAll) }
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState()
            uiState.errorMessage != null && uiState.gridData == null -> ErrorState(
                message = uiState.errorMessage,
                onDismiss = { onAction(TestingGridAction.OnDismissError) }
            )
            uiState.phase == TestingPhase.EVENT_DETAIL -> EventDetailPhase(uiState, onAction, padding)
            else -> LiveEntryPhase(uiState, eventId, groupId, onAction, padding)
        }
    }

    uiState.editingCell?.let { cell ->
        ScoreEntryDialog(
            athleteName = "${cell.athlete.firstName} ${cell.athlete.lastName}",
            testName = cell.test.name,
            unit = cell.test.unit,
            testDescription = cell.test.description,
            inputParadigm = cell.test.inputParadigm,
            validMin = cell.test.validMin,
            validMax = cell.test.validMax,
            currentResult = cell.currentResult,
            pendingScore = cell.pendingScore,
            onDismiss = { onAction(TestingGridAction.OnDismissEditing) },
            onSave = { score -> onAction(TestingGridAction.OnSaveScore(score)) },
            onClearPending = { onAction(TestingGridAction.OnClearPendingForEditingCell) },
            onDeleteSaved = {
                cell.currentResult?.let { result ->
                    onAction(TestingGridAction.OnRequestDelete(cell.athlete, cell.test, result.id))
                }
            }
        )
    }
}

@Composable
private fun SubmitBar(pendingCount: Int, isSubmitting: Boolean, onSubmit: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = NavyPrimary,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$pendingCount unsaved score${if (pendingCount == 1) "" else "s"}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Tap submit to save to the database",
                    color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = NavyPrimary
                )
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = NavyPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Saving…", fontWeight = FontWeight.Bold)
                } else {
                    Text("Submit", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DiscardDraftsDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard unsaved scores?") },
        text = {
            Text(
                "$count score${if (count == 1) "" else "s"} ${if (count == 1) "has" else "have"} not been submitted. " +
                    "Leaving now will discard ${if (count == 1) "it" else "them"}."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Discard", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep editing") }
        }
    )
}

@Composable
private fun DeleteResultDialog(
    athleteName: String,
    testName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete result?") },
        text = {
            Text(
                "Permanently delete $athleteName's $testName result? " +
                    "This cannot be undone and will update reports immediately."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun LiveEntryPhase(
    uiState: TestingGridUiState,
    eventId: String,
    groupId: String,
    onAction: (TestingGridAction) -> Unit,
    padding: PaddingValues
) {
    val gridData = uiState.gridData ?: return
    val horizontalScroll = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        // Progress counts saved (DB) + pending so coaches see drafts contributing.
        val savedAthletes = gridData.students.count { athlete ->
            gridData.results.any { it.individualId == athlete.id }
        }
        val pendingAthletes = uiState.pendingResults.keys
            .map { it.first }
            .toSet()
            .filter { id -> gridData.results.none { it.individualId == id } }
            .size
        TestingProgressBanner(
            totalAthletes = gridData.students.size,
            testedAthletes = savedAthletes,
            pendingAthletes = pendingAthletes
        )

        Row(modifier = Modifier.fillMaxWidth().background(NavyPrimary)) {
            Box(modifier = Modifier.width(140.dp).padding(14.dp)) {
                Text("ATHLETE", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Color.White, letterSpacing = 1.sp)
            }
            Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                gridData.tests.forEach { test ->
                    Box(modifier = Modifier.width(100.dp).padding(vertical = 14.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
                        Text(test.name.uppercase(), fontWeight = FontWeight.ExtraBold, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, color = Color.White, lineHeight = 12.sp)
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(gridData.students.indices.toList()) { index ->
                val athlete = gridData.students[index]
                val rowBg = if (index % 2 == 0) Color.White else Color(0xFFF9FAFB)

                AthleteRow(
                    athlete = athlete,
                    tests = gridData.tests,
                    results = gridData.results,
                    pendingResults = uiState.pendingResults,
                    horizontalScroll = horizontalScroll,
                    modifier = Modifier.background(rowBg),
                    onCellClick = { test ->
                        if (test.timingMode != TimingMode.MANUAL_ENTRY) {
                            onAction(TestingGridAction.OnNavigateToStopwatch(eventId, test.id, groupId, athlete.id))
                        } else {
                            onAction(TestingGridAction.OnStartEditing(athlete, test))
                        }
                    },
                    onCellLongPress = { test ->
                        gridData.results.firstOrNull { it.individualId == athlete.id && it.testId == test.id }?.let { saved ->
                            onAction(TestingGridAction.OnRequestDelete(athlete, test, saved.id))
                        }
                    },
                    onAthleteClick = { onAction(TestingGridAction.OnNavigateToAthleteReport(athlete.id)) }
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AthleteRow(
    athlete: Individual,
    tests: List<FitnessTest>,
    results: List<TestResult>,
    pendingResults: Map<Pair<String, String>, Double>,
    horizontalScroll: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier,
    onCellClick: (FitnessTest) -> Unit,
    onCellLongPress: (FitnessTest) -> Unit,
    onAthleteClick: () -> Unit
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.width(140.dp)
                .combinedClickable(onClick = onAthleteClick)
                .padding(14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (athlete.isRestricted || athlete.medicalAlert != null) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text("${athlete.lastName}, ${athlete.firstName.first()}.", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
            tests.forEach { test ->
                val savedResult = results.find { it.individualId == athlete.id && it.testId == test.id }
                val pendingScore = pendingResults[athlete.id to test.id]
                ScoreCell(
                    savedResult = savedResult,
                    pendingScore = pendingScore,
                    onClick = { onCellClick(test) },
                    onLongPress = { onCellLongPress(test) }
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ScoreCell(
    savedResult: TestResult?,
    pendingScore: Double?,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val (bgColor, textColor) = when {
        pendingScore != null -> PendingTint to Color(0xFF5D4037)
        savedResult == null -> Color.Transparent to Color.Gray
        savedResult.percentile == null -> PerformanceGrey to Color.Black
        savedResult.percentile >= 70 -> PerformanceGreen.copy(alpha = 0.7f) to PerformanceGreenText
        savedResult.percentile >= 35 -> PerformanceYellow.copy(alpha = 0.7f) to PerformanceYellowText
        else -> PerformanceRed.copy(alpha = 0.7f) to PerformanceRedText
    }

    Box(
        modifier = Modifier
            .width(100.dp)
            .height(52.dp)
            .background(bgColor)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        contentAlignment = Alignment.Center
    ) {
        when {
            pendingScore != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        String.format(Locale.getDefault(), "%.1f", pendingScore),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        "DRAFT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PendingBorder,
                        letterSpacing = 1.sp
                    )
                }
            }
            savedResult != null -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(String.format(Locale.getDefault(), "%.1f", savedResult.rawScore), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textColor)
                    savedResult.percentile?.let { p -> Text("${p}%", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = textColor.copy(alpha = 0.8f)) }
                }
            }
            else -> Text("--", color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ScoreEntryDialog(
    athleteName: String,
    testName: String,
    unit: String,
    testDescription: String?,
    inputParadigm: com.example.alearning.domain.model.standards.InputParadigm,
    currentResult: TestResult?,
    pendingScore: Double?,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    onClearPending: () -> Unit,
    onDeleteSaved: () -> Unit,
    validMin: Double? = null,
    validMax: Double? = null
) {
    val initial = pendingScore ?: currentResult?.rawScore
    var scoreText by remember { mutableStateOf(initial?.toString() ?: "") }

    val isInRange = remember(scoreText, validMin, validMax) {
        val v = scoreText.toDoubleOrNull() ?: return@remember false
        val minOk = validMin?.let { v >= it } ?: true
        val maxOk = validMax?.let { v <= it } ?: true
        minOk && maxOk
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(athleteName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("$testName · $unit", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (pendingScore != null) {
                        Text(
                            "Editing unsaved draft",
                            style = MaterialTheme.typography.labelSmall,
                            color = PendingBorder,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Surface(modifier = Modifier.fillMaxWidth().height(72.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(scoreText.ifEmpty { "—" }, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    }
                }
                if (!isInRange && scoreText.isNotEmpty()) {
                    Text(
                        text = buildString {
                            append("Valid range: ")
                            if (validMin != null) append("≥ $validMin")
                            if (validMin != null && validMax != null) append(" and ")
                            if (validMax != null) append("≤ $validMax")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TestInputSwitcher(
                    paradigm = inputParadigm,
                    currentValue = scoreText,
                    onValueChange = { scoreText = it },
                    onSubmit = {
                        if (isInRange) {
                            scoreText.toDoubleOrNull()?.let { onSave(it) }
                        }
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pendingScore != null) {
                        TextButton(onClick = onClearPending, modifier = Modifier.weight(1f)) {
                            Text("Clear draft")
                        }
                    }
                    if (currentResult != null) {
                        TextButton(onClick = onDeleteSaved, modifier = Modifier.weight(1f)) {
                            Text("Delete saved", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    )
}

@Composable
private fun TestingProgressBanner(totalAthletes: Int, testedAthletes: Int, pendingAthletes: Int) {
    val progress = if (totalAthletes > 0) testedAthletes.toFloat() / totalAthletes else 0f
    Surface(modifier = Modifier.fillMaxWidth(), color = Color(0xFFF3F4F6)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.weight(1f).height(8.dp).clip(CircleShape), color = NavyPrimary)
            Column(horizontalAlignment = Alignment.End) {
                Text("$testedAthletes / $totalAthletes Saved", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                if (pendingAthletes > 0) {
                    Text("+$pendingAthletes pending", style = MaterialTheme.typography.labelSmall, color = PendingBorder)
                }
            }
        }
    }
}

@Composable
private fun LoadingState() { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = NavyPrimary) } }

@Composable
private fun ErrorState(message: String, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, null, Modifier.size(48.dp), MaterialTheme.colorScheme.error)
            Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun EventDetailPhase(uiState: TestingGridUiState, onAction: (TestingGridAction) -> Unit, padding: PaddingValues) {
    val gridData = uiState.gridData
    Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = NavyPrimary)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(uiState.event?.name ?: "Testing Event", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${gridData?.students?.size ?: 0} Athletes • ${gridData?.tests?.size ?: 0} Tests", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(onClick = { onAction(TestingGridAction.OnStartTesting) }, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)) {
            Text("Begin Testing Session", fontWeight = FontWeight.Bold)
        }
    }
}
