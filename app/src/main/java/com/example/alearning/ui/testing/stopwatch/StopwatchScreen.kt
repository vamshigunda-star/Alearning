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
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay

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
                    onSubmit = { onAction(StopwatchAction.OnRequestSubmit) }
                )
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingBox(padding)
            uiState.errorMessage != null -> ErrorBox(uiState.errorMessage, onAction, padding)
            uiState.isSessionComplete -> {
                // Navigate back automatically handled by LaunchedEffect
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.mode == TimingMode.INDIVIDUAL -> IndividualModeContent(uiState, allAthletes, onAction, padding)
            uiState.mode == TimingMode.GROUP_START -> GroupStartModeContent(uiState, onAction, padding)
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

// Removed SessionCompleteContent

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

    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
        Spacer(Modifier.height(16.dp))

        // Sticky Stopwatch display
        StopwatchDisplay(elapsedMs = uiState.elapsedMs)

        Spacer(Modifier.height(16.dp))

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

            Spacer(Modifier.height(8.dp))

            // Main action button
            if (uiState.stopwatchPhase != StopwatchPhase.CONFIRMING) {
                val isRunning = uiState.stopwatchPhase == StopwatchPhase.RUNNING
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .kineticPulse(
                            shape = RoundedCornerShape(16.dp),
                            baseElevation = 4.dp,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAction(StopwatchAction.OnStartStop)
                            }
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRunning) MaterialTheme.colorScheme.error else ElectricBlue
                    )
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            if (isRunning) "STOP" else "START",
                            fontSize = 24.sp,
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
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 24.dp).fillMaxWidth()
            ) {
                Text(
                    "Trial completed. Proceed to next trial.",
                    modifier = Modifier.padding(12.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = PerformanceGreenText
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Testing Grid Labels
        TestingGridLabels(trialCount = totalTrials)

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(allAthletes) { athlete ->
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

@Composable
private fun GroupStartModeContent(uiState: StopwatchUiState, onAction: (StopwatchAction) -> Unit, padding: PaddingValues) {
    val haptic = LocalHapticFeedback.current
    Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Heat ${uiState.currentHeatNumber} of ${uiState.totalHeats}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(16.dp))
        StopwatchDisplay(elapsedMs = uiState.elapsedMs)
        Spacer(Modifier.height(16.dp))
        
        if (uiState.stopwatchPhase == StopwatchPhase.READY) {
            Button(
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onAction(StopwatchAction.OnStartStop) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(80.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PerformanceGreen),
                shape = RoundedCornerShape(16.dp)
            ) { Text("START SESSION", fontSize = 28.sp, fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(16.dp))
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp)) {
            items(uiState.heatAthletes) { athlete ->
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
        color = MaterialTheme.colorScheme.onSurface
    )
}

// Removed ConfirmationFlash

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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
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
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(if (isSelected) 3.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isAbsent) 0.dp else if (isSelected) 4.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
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
                    lineHeight = 12.sp,
                    color = if (isAbsent) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )
                
                if (isAbsent) {
                    Text("Absent", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }

                // Absent toggle
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Absent?", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                    Spacer(Modifier.width(4.dp))
                    Switch(
                        checked = isAbsent,
                        onCheckedChange = { onToggleAbsent() },
                        modifier = Modifier.height(24.dp).scale(0.6f)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Trial Chips (Right Column)
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(athlete.totalTrials) { index ->
                    val trialNumber = index + 1
                    val historicalResult = athlete.historicalTrials.getOrNull(index)
                    
                    val status = when {
                        historicalResult != null -> AthleteStatus.COMPLETED
                        trialNumber == athlete.currentTrial && athlete.status == AthleteStatus.CAPTURED -> AthleteStatus.CAPTURED
                        trialNumber == athlete.currentTrial && athlete.status == AthleteStatus.ABSENT -> AthleteStatus.ABSENT
                        else -> AthleteStatus.WAITING
                    }
                    
                    val displayTimeMs = when (status) {
                        AthleteStatus.COMPLETED -> (historicalResult!!.rawScore * 1000).toLong()
                        AthleteStatus.CAPTURED -> athlete.capturedTimeMs
                        else -> null
                    }
                    
                    val resultId = historicalResult?.id
                    
                    val chipAlpha = if (isAbsent) 0.3f else 1f
                    
                    TrialChip(
                        status = status,
                        timeMs = displayTimeMs,
                        modifier = Modifier.weight(1f).alpha(chipAlpha)
                            .combinedClickable(
                                onClick = { if (!isAbsent) onClick() },
                                onLongClick = {
                                    if (!isAbsent && (status == AthleteStatus.COMPLETED || status == AthleteStatus.CAPTURED)) {
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
private fun TrialChip(status: AthleteStatus, timeMs: Long?, modifier: Modifier = Modifier) {
    val (bgColor, borderColor, iconColor) = when (status) {
        AthleteStatus.COMPLETED -> Triple(PerformanceGreen.copy(alpha = 0.1f), PerformanceGreen, PerformanceGreenText)
        AthleteStatus.CAPTURED -> Triple(PerformanceYellow.copy(alpha = 0.15f), PerformanceYellow, PerformanceYellowText)
        else -> Triple(Color.LightGray.copy(alpha = 0.1f), Color.LightGray.copy(alpha = 0.5f), Color.Gray)
    }

    Surface(
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(if (status == AthleteStatus.WAITING) 1.dp else 2.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (status == AthleteStatus.COMPLETED) Icons.Default.Check else Icons.Default.Timer,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            if (timeMs != null) {
                Spacer(Modifier.width(4.dp))
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
