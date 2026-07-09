package com.example.alearning.ui.testing.stopwatch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
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
import com.example.alearning.domain.model.standards.TimingMode
import com.example.alearning.ui.components.AppTopBar
import com.example.alearning.ui.theme.*

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo

@Composable
fun StopwatchScreen(
    onNavigateBack: () -> Unit,
    viewModel: StopwatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Intercept system back so unsaved drafts trigger the discard dialog before leaving.
    BackHandler(enabled = uiState.hasPendingChanges) {
        viewModel.onAction(StopwatchAction.OnRequestBack)
    }

    StopwatchContent(
        uiState = uiState,
        allAthletes = viewModel.getAllAthletes(),
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
    onAction: (StopwatchAction) -> Unit
) {
    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(NavyPrimary)) {
                AppTopBar(
                    title = {
                        Column {
                            Text(
                                uiState.testName,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            if (uiState.eventName.isNotEmpty()) {
                                Text(
                                    uiState.eventName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { onAction(StopwatchAction.OnNavigateBack) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    actions = {
                        // Compact Timer Badge in Header
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    Modifier.size(8.dp).background(
                                        if (uiState.stopwatchPhase == StopwatchPhase.RUNNING) SportOrange else Color.Gray,
                                        CircleShape
                                    )
                                )
                                Text(
                                    formatTimeDisplay(uiState.elapsedMs),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                )

                // Progress Section
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val progress = if (uiState.totalCount > 0) uiState.completedCount.toFloat() / uiState.totalCount else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.width(100.dp).height(8.dp),
                            color = SportBlue,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            strokeCap = StrokeCap.Round
                        )
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Text(
                                "${uiState.completedCount} of ${uiState.totalCount} Saved",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        },
        bottomBar = {
            if (uiState.hasPendingChanges) {
                SubmitBar(
                    pendingCount = uiState.pendingResults.size,
                    isSubmitting = uiState.isSubmitting,
                    onSubmit = { onAction(StopwatchAction.OnSubmitPending) }
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingBox(padding)
            uiState.errorMessage != null -> ErrorBox(uiState.errorMessage, onAction, padding)
            uiState.isSessionComplete -> SessionCompleteContent(uiState, onAction, padding)
            uiState.mode == TimingMode.INDIVIDUAL -> IndividualModeContent(uiState, allAthletes, onAction, padding)
            uiState.mode == TimingMode.GROUP_START -> GroupStartModeContent(uiState, onAction, padding)
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
private fun SessionCompleteContent(uiState: StopwatchUiState, onAction: (StopwatchAction) -> Unit, padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Check, null, modifier = Modifier.size(80.dp).background(PerformanceGreen, CircleShape).padding(16.dp), tint = Color.White)
        Spacer(Modifier.height(24.dp))
        Text("All Done!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("${uiState.completedCount} results recorded for ${uiState.testName}", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = { onAction(StopwatchAction.OnNavigateBack) }, modifier = Modifier.fillMaxWidth()) { Text("Back to Testing Grid") }
    }
}

@Composable
private fun IndividualModeContent(
    uiState: StopwatchUiState,
    allAthletes: List<AthleteQueueItem>,
    onAction: (StopwatchAction) -> Unit,
    padding: PaddingValues
) {
    val haptic = LocalHapticFeedback.current
    val currentAthlete = allAthletes.find { it.athleteId == uiState.selectedAthleteId }
    val currentIndex = allAthletes.indexOfFirst { it.athleteId == uiState.selectedAthleteId }
    
    val nextWaitingAthlete = if (currentIndex != -1) {
        allAthletes.drop(currentIndex + 1).find { it.status == AthleteStatus.WAITING }
            ?: allAthletes.take(currentIndex).find { it.status == AthleteStatus.WAITING }
    } else {
        allAthletes.find { it.status == AthleteStatus.WAITING }
    }

    val totalTrials = allAthletes.firstOrNull()?.totalTrials ?: 1

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Spacer(Modifier.height(24.dp))

            // Stopwatch display
            StopwatchDisplay(elapsedMs = uiState.elapsedMs)

            Spacer(Modifier.height(24.dp))

            // Selected Athlete Info & Action
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

                Spacer(Modifier.height(16.dp))

                // Main action button
                if (uiState.stopwatchPhase != StopwatchPhase.CONFIRMING) {
                    val isRunning = uiState.stopwatchPhase == StopwatchPhase.RUNNING
                    Button(
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAction(StopwatchAction.OnStartStop) 
                        },
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) MaterialTheme.colorScheme.error else PerformanceGreen
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = currentAthlete != null
                    ) { 
                        Text(if (isRunning) "STOP" else "START", fontSize = 20.sp, fontWeight = FontWeight.Bold) 
                    }
                }
            }

            // Confirmation overlay
            AnimatedVisibility(visible = uiState.stopwatchPhase == StopwatchPhase.CONFIRMING, enter = fadeIn(), exit = fadeOut()) {
                uiState.confirmationData?.let { data ->
                    ConfirmationFlash(
                        data = data,
                        canUndo = uiState.canUndo,
                        hasPending = uiState.hasPendingChanges,
                        isSubmitting = uiState.isSubmitting,
                        onUndo = { onAction(StopwatchAction.OnUndo) },
                        onSubmit = { onAction(StopwatchAction.OnSubmitPending) },
                        onNext = { onAction(StopwatchAction.OnNext) },
                        nextAthleteName = nextWaitingAthlete?.name,
                        onResetAthlete = { onAction(StopwatchAction.OnResetAthlete) }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Testing Grid Labels
            TestingGridLabels(trialCount = totalTrials)
        }

        items(allAthletes) { athlete ->
            AthleteGridCard(
                athlete = athlete,
                isSelected = athlete.athleteId == uiState.selectedAthleteId,
                onClick = { onAction(StopwatchAction.OnSelectAthlete(athlete.athleteId)) }
            )
        }
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
        AnimatedVisibility(visible = uiState.stopwatchPhase == StopwatchPhase.CONFIRMING, enter = fadeIn(), exit = fadeOut()) {
            uiState.confirmationData?.let { data ->
                ConfirmationFlash(
                    data = data,
                    canUndo = false,
                    hasPending = uiState.hasPendingChanges,
                    isSubmitting = uiState.isSubmitting,
                    onUndo = {},
                    onSubmit = { onAction(StopwatchAction.OnSubmitPending) },
                    onNext = { onAction(StopwatchAction.OnNext) },
                    nextAthleteName = null
                )
            }
        }
        if (uiState.stopwatchPhase != StopwatchPhase.CONFIRMING) {
            when (uiState.stopwatchPhase) {
                StopwatchPhase.READY -> {
                    Button(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onAction(StopwatchAction.OnStartStop) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(80.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PerformanceGreen),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("START HEAT", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                }
                StopwatchPhase.RUNNING -> {
                    Column(modifier = Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onAction(StopwatchAction.OnFinishTap) },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("TAP TO FINISH", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
                        
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { onAction(StopwatchAction.OnResetHeat) }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                                Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("RESET")
                            }
                            OutlinedButton(onClick = { onAction(StopwatchAction.OnStopHeat) }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(12.dp)) {
                                Text("STOP HEAT")
                            }
                        }
                    }
                }
                else -> {}
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Select Athlete who is finishing:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp)) {
            items(uiState.heatAthletes) { athlete ->
                HeatAthleteRow(athlete, isSelected = uiState.selectedAthleteId == athlete.athleteId) {
                    onAction(StopwatchAction.OnSelectAthlete(athlete.athleteId))
                }
            }
        }
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
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun ConfirmationFlash(
    data: ConfirmationData,
    canUndo: Boolean,
    hasPending: Boolean,
    isSubmitting: Boolean,
    onUndo: () -> Unit,
    onSubmit: () -> Unit,
    onNext: () -> Unit,
    nextAthleteName: String?,
    onResetAthlete: (() -> Unit)? = null
) {
    val containerColor = if (hasPending) PerformanceYellow.copy(alpha = 0.15f) else PerformanceGreen.copy(alpha = 0.15f)
    val accentColor = if (hasPending) PerformanceYellowText else PerformanceGreenText

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = formatTimeDisplay(data.timeMs),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = accentColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (hasPending) "staged for ${data.athleteName}" else "✓ saved for ${data.athleteName}",
                style = MaterialTheme.typography.bodyLarge,
                color = accentColor,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (hasPending) "Tap SUBMIT to save before continuing" else "Result recorded. Tap DONE or NEXT to continue.",
                style = MaterialTheme.typography.labelSmall,
                color = accentColor.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))

            // Undo / Reset row — only relevant while staged & not submitting
            if (hasPending && !isSubmitting && (canUndo || onResetAthlete != null)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (canUndo) {
                        OutlinedButton(
                            onClick = onUndo,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Undo, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Clear")
                        }
                    }
                    if (onResetAthlete != null) {
                        OutlinedButton(
                            onClick = onResetAthlete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Primary action row: SUBMIT (gating) → then DONE / NEXT
            if (hasPending) {
                Button(
                    onClick = onSubmit,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Saving…", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text("SUBMIT", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onNext, modifier = Modifier.weight(1f)) {
                        Text("DONE")
                    }
                    if (nextAthleteName != null) {
                        Button(
                            onClick = onNext,
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = PerformanceGreen)
                        ) {
                            Icon(Icons.Default.SkipNext, null)
                            Spacer(Modifier.width(8.dp))
                            Text("NEXT: $nextAthleteName", maxLines = 1, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TestingGridLabels(trialCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(96.dp)) // Matches athlete info width
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(trialCount) { index ->
                Text(
                    text = "TRIAL\n${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    lineHeight = 12.sp
                )
            }
        }
    }
}

@Composable
private fun AthleteGridCard(athlete: AthleteQueueItem, isSelected: Boolean, onClick: () -> Unit) {
    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.White
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, borderColor) else BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Athlete Info (Left Column)
            Column(
                modifier = Modifier.width(96.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Avatar
                val initials = athlete.name.split(" ").let { parts ->
                    if (parts.size >= 2) "${parts[0].take(1)}${parts[1].take(1)}"
                    else athlete.name.take(2)
                }.uppercase()
                
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            initials,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Text(
                    athlete.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 12.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            // Trial Chips (Right Column)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(athlete.totalTrials) { index ->
                    val trialNumber = index + 1
                    val status = when {
                        trialNumber < athlete.currentTrial -> AthleteStatus.COMPLETED
                        trialNumber == athlete.currentTrial && athlete.status == AthleteStatus.CAPTURED -> AthleteStatus.CAPTURED
                        else -> AthleteStatus.WAITING
                    }
                    
                    TrialChip(
                        status = status,
                        timeMs = if (status == AthleteStatus.CAPTURED) athlete.capturedTimeMs else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TrialChip(status: AthleteStatus, timeMs: Long?, modifier: Modifier = Modifier) {
    val (bgColor, borderColor, iconColor) = when (status) {
        AthleteStatus.COMPLETED -> Triple(PerformanceGreen.copy(alpha = 0.1f), PerformanceGreen, PerformanceGreenText)
        AthleteStatus.CAPTURED -> Triple(PerformanceYellow.copy(alpha = 0.15f), PerformanceYellow, PerformanceYellowText)
        else -> Triple(Color.LightGray.copy(alpha = 0.1f), Color.LightGray.copy(alpha = 0.5f), Color.Gray)
    }

    Surface(
        modifier = modifier.height(64.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
        border = BorderStroke(if (status == AthleteStatus.WAITING) 1.dp else 2.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (status == AthleteStatus.COMPLETED) Icons.Default.Check else Icons.Default.Timer,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            if (timeMs != null) {
                Text(
                    formatTimeDisplay(timeMs),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
            }
        }
    }
}

@Composable
private fun HeatAthleteRow(athlete: AthleteQueueItem, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        athlete.status == AthleteStatus.CAPTURED -> PerformanceGreen.copy(alpha = 0.1f)
        athlete.status == AthleteStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    val border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = bgColor,
        shape = RoundedCornerShape(8.dp),
        border = border
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (athlete.status == AthleteStatus.CAPTURED || athlete.status == AthleteStatus.COMPLETED) {
                Icon(Icons.Default.Check, null, tint = if (athlete.status == AthleteStatus.CAPTURED) PerformanceGreenText else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            } else { Spacer(Modifier.size(20.dp)) }
            Spacer(Modifier.width(8.dp))
            Text(athlete.name, style = MaterialTheme.typography.bodyMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
            if (athlete.capturedTimeMs != null) {
                Text(formatTimeDisplay(athlete.capturedTimeMs), style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium, color = PerformanceGreenText)
            } else if (athlete.status == AthleteStatus.ACTIVE) {
                Text(if (isSelected) "READY" else "waiting...", style = MaterialTheme.typography.labelSmall, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatTimeDisplay(ms: Long): String {
    val minutes = (ms / 60000).toInt()
    val seconds = ((ms % 60000) / 1000).toInt()
    val centis = ((ms % 1000) / 10).toInt()
    return if (minutes > 0) "%d:%02d.%02d".format(minutes, seconds, centis) else "%d.%02d".format(seconds, centis)
}
