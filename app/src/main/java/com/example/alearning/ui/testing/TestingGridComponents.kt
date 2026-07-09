package com.example.alearning.ui.testing

import android.util.Log
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.alearning.ui.components.AppTopBar
import com.example.alearning.ui.components.AppTopBarSubtitleColor
import com.example.alearning.ui.theme.*
import com.example.alearning.ui.components.testing.TestInputSwitcher
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun SubmitBar(pendingCount: Int, isSubmitting: Boolean, onSubmit: () -> Unit) {
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
fun DiscardDraftsDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
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
fun TimingChoiceDialog(
    athleteName: String,
    testName: String,
    unit: String,
    onUseStopwatch: () -> Unit,
    onEnterManually: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How are you recording this time?") },
        text = {
            Column {
                Text(
                    "$athleteName · $testName ($unit)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Use the in-app stopwatch for live timing, or enter the time manually if you used an external timer.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onUseStopwatch,
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
            ) {
                Text("Use Stopwatch", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onEnterManually) {
                Text("Enter Manually")
            }
        }
    )
}

@Composable
fun DeleteResultDialog(
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
fun LiveEntryPhase(
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
        val savedAthletes = remember(gridData.students, gridData.results) {
            gridData.students.count { athlete ->
                gridData.results.any { it.individualId == athlete.id }
            }
        }
        val pendingAthletes = remember(uiState.pendingResults, gridData.results) {
            uiState.pendingResults.keys
                .map { it.first }
                .toSet()
                .filter { id -> gridData.results.none { it.individualId == id } }
                .size
        }
        TestingProgressBanner(
            totalAthletes = gridData.students.size,
            testedAthletes = savedAthletes,
            pendingAthletes = pendingAthletes
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(gridData.students) { athlete ->
                AthleteCard(
                    athlete = athlete,
                    tests = gridData.tests,
                    results = gridData.results,
                    pendingResults = uiState.pendingResults,
                    onCellClick = { test ->
                        Log.d("TestingGridComponents", "Cell clicked for athlete ${athlete.id}, test ${test.id}, timingMode ${test.timingMode}")
                        if (test.timingMode != TimingMode.MANUAL_ENTRY) {
                            when (uiState.testCapturePreferences[test.id]) {
                                CaptureMethodPreference.STOPWATCH -> {
                                    onAction(TestingGridAction.OnNavigateToStopwatch(eventId, test.id, groupId, athlete.id))
                                }
                                CaptureMethodPreference.MANUAL -> {
                                    onAction(TestingGridAction.OnStartEditing(athlete, test))
                                }
                                null -> {
                                    onAction(TestingGridAction.OnRequestTimingChoice(athlete, test))
                                }
                            }
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
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AthleteCard(
    athlete: Individual,
    tests: List<FitnessTest>,
    results: List<TestResult>,
    pendingResults: Map<Pair<String, String>, Double>,
    modifier: Modifier = Modifier,
    onCellClick: (FitnessTest) -> Unit,
    onCellLongPress: (FitnessTest) -> Unit,
    onAthleteClick: () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NavyPrimary)
                    .combinedClickable(onClick = onAthleteClick)
                    .padding(12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (athlete.isRestricted || athlete.medicalAlert != null) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Medical alert",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        "${athlete.firstName} ${athlete.lastName}", 
                        fontSize = 14.sp, 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White,
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                tests.forEach { test ->
                    val savedResult = results.find { it.individualId == athlete.id && it.testId == test.id }
                    val pendingScore = pendingResults[athlete.id to test.id]
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            test.name.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(bottom = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
    }
}

@OptIn(ExperimentalComposeUiApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ScoreCell(
    savedResult: TestResult?,
    pendingScore: Double?,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val (bgColor, textColor) = when {
        pendingScore != null -> PendingTint to Color(0xFF5D4037)
        savedResult == null -> Color.Transparent to Color.Gray
        savedResult.percentile == null -> PerformanceGrey to Color.Black
        savedResult.percentile >= 60 -> PerformanceGreen.copy(alpha = 0.7f) to PerformanceGreenText
        savedResult.percentile >= 30 -> PerformanceYellow.copy(alpha = 0.7f) to PerformanceYellowText
        else -> PerformanceRed.copy(alpha = 0.7f) to PerformanceRedText
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(8.dp))
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
fun ScoreEntryDialog(
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
    val scrollState = rememberScrollState()

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        dismissButton = null
    )
}

@Composable
fun TestingProgressBanner(totalAthletes: Int, testedAthletes: Int, pendingAthletes: Int) {
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
fun LoadingState() { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = NavyPrimary) } }

@Composable
fun ErrorState(message: String, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, null, Modifier.size(48.dp), MaterialTheme.colorScheme.error)
            Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
fun EventDetailPhase(uiState: TestingGridUiState, onAction: (TestingGridAction) -> Unit, padding: PaddingValues) {
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
