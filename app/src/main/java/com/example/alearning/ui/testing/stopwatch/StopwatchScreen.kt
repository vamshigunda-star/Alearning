@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.alearning.ui.testing.stopwatch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.alearning.domain.model.standards.TimingMode
import com.example.alearning.ui.components.AppTopBar
import com.example.alearning.ui.components.AppTopBarSubtitleColor
import com.example.alearning.ui.components.InlineErrorBanner
import com.example.alearning.ui.theme.*

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay

@Composable
fun StopwatchScreen(
    onNavigateBack: () -> Unit,
    viewModel: StopwatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Intercept system back so unsaved drafts trigger the discard dialog before leaving.
    BackHandler(enabled = uiState.hasPendingChanges) {
        viewModel.onAction(StopwatchAction.OnRequestBack)
    }

    LaunchedEffect(viewModel) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is StopwatchUiEvent.ScrollToAthlete -> {
                    val index = uiState.allAthletes.indexOfFirst { it.athleteId == event.athleteId }
                    if (index >= 0) {
                        val alreadyVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == index }
                        if (!alreadyVisible) {
                            listState.animateScrollToItem(index)
                        }
                    }
                }
                is StopwatchUiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    StopwatchContent(
        uiState = uiState,
        allAthletes = uiState.allAthletes,
        listState = listState,
        snackbarHostState = snackbarHostState,
        onAction = { action ->
            when (action) {
                is StopwatchAction.OnNavigateBack -> {
                    if (uiState.hasPendingChanges) {
                        viewModel.onAction(StopwatchAction.OnRequestBack)
                    } else {
                        onNavigateBack()
                    }
                }
                else -> viewModel.onAction(action)
            }
        }
    )

    // Auto-navigate back when the session is marked complete in the ViewModel.
    LaunchedEffect(uiState.isSessionComplete) {
        if (uiState.isSessionComplete) {
            onNavigateBack()
        }
    }

    if (uiState.showDiscardDialog) {
        DiscardPendingDialog(
            count = uiState.pendingResults.size,
            onConfirm = {
                viewModel.onAction(StopwatchAction.OnConfirmDiscard)
                onNavigateBack()
            },
            onDismiss = { viewModel.onAction(StopwatchAction.OnDismissDiscard) }
        )
    }
}

@Composable
private fun DiscardPendingDialog(count: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard unsaved times?") },
        text = {
            Text(
                "$count time${if (count == 1) "" else "s"} ${if (count == 1) "has" else "have"} not been submitted. " +
                    "Leaving now will discard ${if (count == 1) "it" else "them"}."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Discard", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Keep editing") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopwatchContent(
    uiState: StopwatchUiState,
    allAthletes: List<AthleteQueueItem>,
    listState: LazyListState,
    snackbarHostState: SnackbarHostState,
    onAction: (StopwatchAction) -> Unit
) {
    Scaffold(
        topBar = {
            StopwatchTopBar(uiState = uiState, onBack = { onAction(StopwatchAction.OnNavigateBack) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (uiState.hasPendingChanges) {
                SubmitBar(
                    pendingCount = uiState.pendingResults.size,
                    isSubmitting = uiState.isSubmitting,
                    onSubmit = { onAction(StopwatchAction.OnRequestSubmit) }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (uiState.errorMessage != null && uiState.sessionLoaded) {
                InlineErrorBanner(
                    message = uiState.errorMessage,
                    onDismiss = { onAction(StopwatchAction.OnDismissError) }
                )
            }
            when {
                uiState.isLoading -> LoadingBox(PaddingValues(0.dp))
                !uiState.sessionLoaded -> ErrorBox(uiState.errorMessage ?: "Something went wrong", onAction, PaddingValues(0.dp))
                uiState.mode == TimingMode.INDIVIDUAL -> IndividualModeContent(uiState, allAthletes, listState, onAction, PaddingValues(0.dp))
                uiState.isSessionComplete -> {
                    // GROUP_START: navigate-back handled by LaunchedEffect in StopwatchScreen
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                uiState.mode == TimingMode.GROUP_START -> GroupStartModeContent(uiState, onAction, PaddingValues(0.dp))
            }
        }
    }

    if (uiState.editingAthleteId != null) {
        val athlete = allAthletes.find { it.athleteId == uiState.editingAthleteId }
        if (athlete != null) {
            val initialTimeMs = if (uiState.editingResultId != null) {
                athlete.historicalTrials.find { it.id == uiState.editingResultId }?.rawScore?.times(1000)?.toLong() ?: 0L
            } else {
                athlete.capturedTimeMs ?: 0L
            }
            EditTimeDialog(
                athleteName = athlete.name,
                initialTimeMs = initialTimeMs,
                onDismiss = { onAction(StopwatchAction.OnCloseEditDialog) },
                onSave = { newTime -> onAction(StopwatchAction.OnSaveEditedTime(athlete.athleteId, newTime, uiState.editingResultId)) },
                onClear = { onAction(StopwatchAction.OnClearEntry(athlete.athleteId, uiState.editingResultId)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopwatchTopBar(uiState: StopwatchUiState, onBack: () -> Unit) {
    Column {
        AppTopBar(
            title = {
                Column {
                    Text(uiState.testName, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        buildSessionContextLine(uiState),
                        style = MaterialTheme.typography.labelMedium,
                        color = AppTopBarSubtitleColor
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )
        SaveProgressIndicator(completedCount = uiState.completedCount, totalCount = uiState.totalCount)
    }
}

private fun buildSessionContextLine(uiState: StopwatchUiState): String {
    val athleteCount = if (uiState.mode == TimingMode.INDIVIDUAL) uiState.allAthletes.size else uiState.heatAthletes.size
    val base = "$athleteCount Athletes • ${uiState.trialsPerAthlete} Trials"
    return if (!uiState.groupName.isNullOrBlank()) "${uiState.groupName} • $base" else base
}

@Composable
private fun SaveProgressIndicator(completedCount: Int, totalCount: Int) {
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.weight(1f).height(8.dp).padding(end = 12.dp),
            color = SportBlue,
            trackColor = SportBlue.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round
        )
        Surface(
            color = PerformanceGreen.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, PerformanceGreen)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = PerformanceGreenText)
                Text(
                    "$completedCount of $totalCount Saved",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = PerformanceGreenText
                )
            }
        }
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
                    Text("Submit All", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LoadingBox(padding: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorBox(message: String, onAction: (StopwatchAction) -> Unit, padding: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onAction(StopwatchAction.OnDismissError) }) { Text("Dismiss") }
        }
    }
}

@Composable
private fun IndividualModeContent(
    uiState: StopwatchUiState,
    allAthletes: List<AthleteQueueItem>,
    listState: LazyListState,
    onAction: (StopwatchAction) -> Unit,
    padding: PaddingValues
) {
    val currentAthlete = allAthletes.find { it.athleteId == uiState.selectedAthleteId }

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        // Sticky control panel: timer, active athlete, start/stop — compressed and centered.
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StopwatchDisplay(elapsedMs = uiState.elapsedMs)

            Spacer(Modifier.height(4.dp))

            if (currentAthlete != null) {
                Text(
                    text = currentAthlete.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Trial ${currentAthlete.currentTrial} of ${currentAthlete.totalTrials}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    text = "Select Athlete to Begin",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(8.dp))

            if (uiState.stopwatchPhase != StopwatchPhase.CONFIRMING) {
                val isRunning = uiState.stopwatchPhase == StopwatchPhase.RUNNING
                val isEnabled = currentAthlete != null || isRunning
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .alpha(if (isEnabled) 1f else 0.5f)
                        .acceleratorClick(
                            onClick = { if (isEnabled) onAction(StopwatchAction.OnStartStop) }
                        ),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.error else ElectricBlue
                    )
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            if (isRunning) "STOP" else "START",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = uiState.showTrialCompletedMessage, enter = fadeIn(), exit = fadeOut()) {
            LaunchedEffect(uiState.showTrialCompletedMessage) {
                if (uiState.showTrialCompletedMessage) {
                    delay(2000) // Show for 2 seconds
                    onAction(StopwatchAction.OnDismissTrialMessage)
                    onAction(StopwatchAction.OnNext) // Automatically move to next waiting athlete
                }
            }
            Surface(
                color = PerformanceGreen.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, PerformanceGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 24.dp).fillMaxWidth()
            ) {
                Text(
                    if (uiState.isSessionComplete) "Result saved. Session complete. Finishing..." else "Result saved. Moving to next athlete...",
                    modifier = Modifier.padding(10.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = PerformanceGreenText
                )
            }
        }

        if (uiState.isSessionComplete) {
            CompletionSummaryCard(
                completedCount = allAthletes.count { it.status == AthleteStatus.COMPLETED },
                absentCount = uiState.absentCount,
                pendingCount = uiState.pendingReviewCount,
                totalCount = allAthletes.size,
                onFinish = { onAction(StopwatchAction.OnNavigateBack) }
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(bottom = 80.dp, top = 4.dp)
        ) {
            items(allAthletes, key = { it.athleteId }) { athlete ->
                if (athlete.status == AthleteStatus.COMPLETED) {
                    CompletedAthleteRow(
                        athlete = athlete,
                        onEditTrial = { resultId -> onAction(StopwatchAction.OnOpenEditDialog(athlete.athleteId, resultId)) }
                    )
                } else {
                    AthleteGridCard(
                        athlete = athlete,
                        isSelected = athlete.athleteId == uiState.selectedAthleteId,
                        onClick = { onAction(StopwatchAction.OnSelectAthlete(athlete.athleteId)) },
                        onEditTrial = { resultId -> onAction(StopwatchAction.OnOpenEditDialog(athlete.athleteId, resultId)) },
                        onToggleAbsent = { onAction(StopwatchAction.OnToggleAbsent(athlete.athleteId)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionSummaryCard(
    completedCount: Int,
    absentCount: Int,
    pendingCount: Int,
    totalCount: Int,
    onFinish: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "$completedCount of $totalCount Athletes Completed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryStat(label = "Completed", value = completedCount, color = PerformanceGreenText)
                SummaryStat(label = "Absent", value = absentCount, color = MaterialTheme.colorScheme.error)
                SummaryStat(label = "Pending", value = pendingCount, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Text("Finish Session", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun GroupStartModeContent(uiState: StopwatchUiState, onAction: (StopwatchAction) -> Unit, padding: PaddingValues) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Heat ${uiState.currentHeatNumber} of ${uiState.totalHeats}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))
        StopwatchDisplay(elapsedMs = uiState.elapsedMs)
        Spacer(Modifier.height(16.dp))

        if (uiState.stopwatchPhase == StopwatchPhase.READY) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(80.dp)
                    .acceleratorClick(
                        onClick = { onAction(StopwatchAction.OnStartStop) }
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32))
            ) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text("START SESSION", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White) } }
        }

        Spacer(Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp)) {
            items(uiState.heatAthletes, key = { it.athleteId }) { athlete ->
                HeatAthleteRow(
                    athlete = athlete,
                    isRunning = uiState.stopwatchPhase == StopwatchPhase.RUNNING,
                    onToggleAbsent = { onAction(StopwatchAction.OnToggleAbsent(athlete.athleteId)) },
                    onCapture = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAction(StopwatchAction.OnCaptureTime(athlete.athleteId))
                    },
                    onEdit = { onAction(StopwatchAction.OnOpenEditDialog(athlete.athleteId)) }
                )
            }
        }

    }

    if (uiState.showMissingEntriesDialog) {
        AlertDialog(
            onDismissRequest = { onAction(StopwatchAction.OnDismissMissingEntriesDialog) },
            title = { Text("Missing Entries") },
            text = { Text("Some athletes do not have recorded times. What would you like to do?") },
            confirmButton = {
                TextButton(onClick = {
                    onAction(StopwatchAction.OnDismissMissingEntriesDialog)
                    onAction(StopwatchAction.OnSubmitPending)
                }) {
                    Text("Leave as Pending")
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(StopwatchAction.OnDismissMissingEntriesDialog) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StopwatchDisplay(elapsedMs: Long) {
    val minutes = (elapsedMs / 60000).toInt()
    val seconds = ((elapsedMs % 60000) / 1000).toInt()
    val centis = ((elapsedMs % 1000) / 10).toInt()

    Text(
        text = "%02d:%02d.%02d".format(minutes, seconds, centis),
        fontSize = 64.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun AthleteGridCard(
    athlete: AthleteQueueItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEditTrial: (String?) -> Unit,
    onToggleAbsent: () -> Unit
) {
    val isAbsent = athlete.status == AthleteStatus.ABSENT
    val containerColor = when {
        isAbsent -> Color.LightGray.copy(alpha = 0.2f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else -> Color.White
    }
    val borderColor = when {
        isAbsent -> Color.Transparent
        isSelected -> MaterialTheme.colorScheme.primary
        else -> Color.LightGray.copy(alpha = 0.3f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable(enabled = !isAbsent) { onClick() },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isSelected) 3.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isAbsent) 0.dp else if (isSelected) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Athlete Info (Left Column) — name + absent toggle
            Column(
                modifier = Modifier.weight(0.35f),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    athlete.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    lineHeight = 14.sp,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    color = if (isAbsent) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )

                if (isAbsent) {
                    Text("Absent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Absent?", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                    Spacer(Modifier.width(4.dp))
                    Switch(
                        checked = isAbsent,
                        onCheckedChange = { onToggleAbsent() },
                        modifier = Modifier.height(20.dp).scale(0.6f)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Trial Chips (Right Column) — trial number integrated into each chip
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(athlete.totalTrials) { index ->
                    val trialNumber = index + 1
                    val historicalResult = athlete.historicalTrials.getOrNull(index)

                    val chipStatus = when {
                        historicalResult != null -> TrialChipStatus.COMPLETED
                        trialNumber == athlete.currentTrial && athlete.status == AthleteStatus.CAPTURED -> TrialChipStatus.IN_PROGRESS
                        else -> TrialChipStatus.NOT_STARTED
                    }

                    val displayTimeMs = when (chipStatus) {
                        TrialChipStatus.COMPLETED -> (historicalResult!!.rawScore * 1000).toLong()
                        TrialChipStatus.IN_PROGRESS -> athlete.capturedTimeMs
                        else -> null
                    }

                    val resultId = historicalResult?.id
                    val chipAlpha = if (isAbsent) 0.3f else 1f

                    TrialChip(
                        trialNumber = trialNumber,
                        status = chipStatus,
                        timeMs = displayTimeMs,
                        modifier = Modifier.weight(1f).alpha(chipAlpha)
                            .combinedClickable(
                                onClick = { if (!isAbsent) onClick() },
                                onLongClick = {
                                    if (!isAbsent && (chipStatus == TrialChipStatus.COMPLETED || chipStatus == TrialChipStatus.IN_PROGRESS)) {
                                        onEditTrial(resultId)
                                    }
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedAthleteRow(
    athlete: AthleteQueueItem,
    onEditTrial: (String?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        color = PerformanceGreen.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, PerformanceGreen.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = PerformanceGreenText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                athlete.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                athlete.historicalTrials.take(athlete.totalTrials).forEach { result ->
                    Text(
                        formatTimeDisplay((result.rawScore * 1000).toLong()),
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = PerformanceGreenText,
                        modifier = Modifier.combinedClickable(
                            onClick = {},
                            onLongClick = { onEditTrial(result.id) }
                        )
                    )
                }
            }
        }
    }
}

private enum class TrialChipStatus { NOT_STARTED, IN_PROGRESS, COMPLETED }

@Composable
private fun TrialChip(trialNumber: Int, status: TrialChipStatus, timeMs: Long?, modifier: Modifier = Modifier) {
    val (bgColor, borderColor, contentColor) = when (status) {
        TrialChipStatus.COMPLETED -> Triple(PerformanceGreen.copy(alpha = 0.1f), PerformanceGreen, PerformanceGreenText)
        TrialChipStatus.IN_PROGRESS -> Triple(SportBlue.copy(alpha = 0.12f), SportBlue, SportBlue)
        TrialChipStatus.NOT_STARTED -> Triple(Color.LightGray.copy(alpha = 0.1f), Color.LightGray.copy(alpha = 0.5f), Color.Gray)
    }

    Surface(
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(if (status == TrialChipStatus.NOT_STARTED) 1.dp else 2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Icon(
                    imageVector = if (status == TrialChipStatus.COMPLETED) Icons.Default.Check else Icons.Default.Timer,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    "T$trialNumber",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
            if (timeMs != null) {
                Text(
                    formatTimeDisplay(timeMs),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun HeatAthleteRow(
    athlete: AthleteQueueItem,
    isRunning: Boolean,
    onToggleAbsent: () -> Unit,
    onCapture: () -> Unit,
    onEdit: () -> Unit
) {
    val (bgColor, borderColor) = when (athlete.status) {
        AthleteStatus.CAPTURED, AthleteStatus.COMPLETED -> Pair(PerformanceGreen.copy(alpha = 0.1f), PerformanceGreen)
        AthleteStatus.ABSENT -> Pair(Color.LightGray.copy(alpha = 0.2f), Color.Transparent)
        else -> Pair(MaterialTheme.colorScheme.surface, Color.LightGray.copy(alpha = 0.5f))
    }

    Surface(
        onClick = {
            if (athlete.status == AthleteStatus.CAPTURED || athlete.status == AthleteStatus.COMPLETED) {
                onEdit()
            } else if (isRunning && athlete.status != AthleteStatus.ABSENT) {
                onCapture()
            }
        },
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = athlete.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (athlete.status == AthleteStatus.ABSENT) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )
                if (athlete.status == AthleteStatus.ABSENT) {
                    Text("Absent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }

            if (athlete.status == AthleteStatus.CAPTURED || athlete.status == AthleteStatus.COMPLETED) {
                Text(
                    text = formatTimeDisplay(athlete.capturedTimeMs ?: 0L),
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = PerformanceGreenText
                )
            } else if (!isRunning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Absent?", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = athlete.status == AthleteStatus.ABSENT,
                        onCheckedChange = { onToggleAbsent() },
                        modifier = Modifier.height(24.dp)
                    )
                }
            } else if (athlete.status != AthleteStatus.ABSENT) {
                Text("TAP TO FINISH", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EditTimeDialog(
    athleteName: String,
    initialTimeMs: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit,
    onClear: () -> Unit
) {
    var inputString by remember { mutableStateOf(formatTimeDisplay(initialTimeMs).replace(Regex("[^0-9]"), "")) }
    val displayMs = inputString.toLongOrNull()?.let { it * 10 } ?: 0L

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Time for $athleteName") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatTimeDisplay(displayMs),
                    style = MaterialTheme.typography.displayMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(24.dp))
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("00", "0", "DEL")
                )
                keys.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        row.forEach { key ->
                            Button(
                                onClick = {
                                    if (key == "DEL") {
                                        if (inputString.isNotEmpty()) inputString = inputString.dropLast(1)
                                    } else {
                                        if (inputString.length < 6) inputString += key
                                    }
                                },
                                modifier = Modifier.size(64.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                Text(key, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(displayMs) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(
                onClick = { onClear(); onDismiss() },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text("Clear Entry") }
        }
    )
}

private fun formatTimeDisplay(ms: Long): String {
    val minutes = (ms / 60000).toInt()
    val seconds = ((ms % 60000) / 1000).toInt()
    val centis = ((ms % 1000) / 10).toInt()
    return if (minutes > 0) "%d:%02d.%02d".format(minutes, seconds, centis) else "%d.%02d".format(seconds, centis)
}
