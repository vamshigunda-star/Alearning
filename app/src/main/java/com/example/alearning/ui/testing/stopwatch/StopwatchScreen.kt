package com.example.alearning.ui.testing.stopwatch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.standards.TimingMode
import com.example.alearning.ui.theme.*

@Composable
fun StopwatchScreen(
    onNavigateBack: () -> Unit,
    viewModel: StopwatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    StopwatchContent(
        uiState = uiState,
        allAthletes = viewModel.getAllAthletes(),
        onAction = { action ->
            when (action) {
                is StopwatchAction.OnNavigateBack -> onNavigateBack()
                else -> viewModel.onAction(action)
            }
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
            TopAppBar(
                title = {
                    Column {
                        Text(uiState.testName, style = MaterialTheme.typography.titleMedium)
                        if (uiState.eventName.isNotEmpty()) {
                            Text(
                                uiState.eventName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(StopwatchAction.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    AssistChip(
                        onClick = {},
                        label = { Text("${uiState.completedCount}/${uiState.totalCount}") },
                        leadingIcon = {
                            Icon(Icons.Default.Timer, null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            )
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
private fun IndividualModeContent(uiState: StopwatchUiState, allAthletes: List<AthleteQueueItem>, onAction: (StopwatchAction) -> Unit, padding: PaddingValues) {
    val haptic = LocalHapticFeedback.current
    val currentAthlete = allAthletes.find { it.athleteId == uiState.selectedAthleteId }
    val nextWaitingAthlete = allAthletes.find { it.status == AthleteStatus.WAITING && it.athleteId != uiState.selectedAthleteId }

    Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(16.dp))

        // Selected Athlete Info
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentAthlete != null) {
                Text(
                    text = currentAthlete.name,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                if (currentAthlete.totalTrials > 1) {
                    Text(
                        text = "Trial ${currentAthlete.currentTrial} of ${currentAthlete.totalTrials}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = "Select Athlete to Begin",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Stopwatch display
        StopwatchDisplay(elapsedMs = uiState.elapsedMs)

        Spacer(Modifier.height(24.dp))

        // Confirmation overlay
        AnimatedVisibility(visible = uiState.stopwatchPhase == StopwatchPhase.CONFIRMING, enter = fadeIn(), exit = fadeOut()) {
            uiState.confirmationData?.let { data ->
                ConfirmationFlash(
                    data = data,
                    canUndo = uiState.canUndo,
                    onUndo = { onAction(StopwatchAction.OnUndo) },
                    onNext = { onAction(StopwatchAction.OnNext) },
                    nextAthleteName = nextWaitingAthlete?.name,
                    onResetAthlete = { onAction(StopwatchAction.OnResetAthlete) }
                )
            }
        }

        // Main action button
        if (uiState.stopwatchPhase != StopwatchPhase.CONFIRMING) {
            val isRunning = uiState.stopwatchPhase == StopwatchPhase.RUNNING
            Button(
                onClick = { 
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onAction(StopwatchAction.OnStartStop) 
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error else PerformanceGreen
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = currentAthlete != null
            ) { 
                Text(if (isRunning) "STOP" else "START", fontSize = 28.sp, fontWeight = FontWeight.Bold) 
            }
        }
        
        Spacer(Modifier.height(24.dp))

        // Athlete List
        Text(
            "Athlete Queue",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
        )
        
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 24.dp)) {
            items(allAthletes) { athlete ->
                IndividualAthleteRow(
                    athlete = athlete,
                    isSelected = uiState.selectedAthleteId == athlete.athleteId,
                    onClick = {
                        if (uiState.stopwatchPhase != StopwatchPhase.RUNNING) {
                            onAction(StopwatchAction.OnSelectAthlete(athlete.athleteId))
                        }
                    }
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
        AnimatedVisibility(visible = uiState.stopwatchPhase == StopwatchPhase.CONFIRMING, enter = fadeIn(), exit = fadeOut()) {
            uiState.confirmationData?.let { data ->
                ConfirmationFlash(data, false, {}, { onAction(StopwatchAction.OnNext) }, null)
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
    onUndo: () -> Unit,
    onNext: () -> Unit,
    nextAthleteName: String?,
    onResetAthlete: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), colors = CardDefaults.cardColors(containerColor = PerformanceGreen.copy(alpha = 0.15f)), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = formatTimeDisplay(data.timeMs), fontSize = 48.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = PerformanceGreenText)
            Spacer(Modifier.height(4.dp))
            Text(text = "saved for ${data.athleteName}", style = MaterialTheme.typography.bodyLarge, color = PerformanceGreenText, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            
            // First row of buttons: Undo & Reset
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (canUndo) {
                    OutlinedButton(onClick = onUndo, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.Undo, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Undo")
                    }
                }
                if (onResetAthlete != null) {
                    OutlinedButton(onClick = onResetAthlete, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                        Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(4.dp)); Text("Reset")
                    }
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            // Second row: DONE or NEXT
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Always show DONE to return to list
                OutlinedButton(onClick = onNext, modifier = Modifier.weight(1f)) {
                    Text("DONE")
                }
                
                // Show NEXT if someone is waiting
                if (nextAthleteName != null) {
                    Button(onClick = onNext, modifier = Modifier.weight(1.5f), colors = ButtonDefaults.buttonColors(containerColor = PerformanceGreen)) {
                        Icon(Icons.Default.SkipNext, null); Spacer(Modifier.width(8.dp)); Text("NEXT: $nextAthleteName", maxLines = 1, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun IndividualAthleteRow(athlete: AthleteQueueItem, isSelected: Boolean, onClick: () -> Unit) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        athlete.status == AthleteStatus.COMPLETED -> PerformanceGreen.copy(alpha = 0.1f)
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
            if (athlete.status == AthleteStatus.COMPLETED) {
                Icon(Icons.Default.Check, null, tint = PerformanceGreenText, modifier = Modifier.size(20.dp))
            } else { 
                Box(Modifier.size(20.dp).background(if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f), CircleShape))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(athlete.name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                if (athlete.totalTrials > 1) {
                    Text("Trials completed: ${athlete.currentTrial - 1}/${athlete.totalTrials}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (isSelected) {
                Text("SELECTED", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
