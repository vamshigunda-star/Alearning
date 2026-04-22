package com.example.alearning.ui.testing.stopwatch

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
        upcomingAthletes = viewModel.getUpcomingAthletes(),
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
    upcomingAthletes: List<AthleteQueueItem>,
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
                    // Progress chip
                    AssistChip(
                        onClick = {},
                        label = { Text("${uiState.completedCount}/${uiState.totalCount}") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
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
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { onAction(StopwatchAction.OnDismissError) }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
            uiState.isSessionComplete -> {
                SessionCompleteContent(uiState = uiState, onAction = onAction, padding = padding)
            }
            uiState.mode == TimingMode.INDIVIDUAL -> {
                IndividualModeContent(
                    uiState = uiState,
                    upcomingAthletes = upcomingAthletes,
                    onAction = onAction,
                    padding = padding
                )
            }
            uiState.mode == TimingMode.GROUP_START -> {
                GroupStartModeContent(uiState = uiState, onAction = onAction, padding = padding)
            }
        }
    }
}

@Composable
private fun SessionCompleteContent(
    uiState: StopwatchUiState,
    onAction: (StopwatchAction) -> Unit,
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .background(PerformanceGreen, CircleShape)
                .padding(16.dp),
            tint = Color.White
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "All Done!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${uiState.completedCount} results recorded for ${uiState.testName}",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onAction(StopwatchAction.OnNavigateBack) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back to Testing Grid")
        }
    }
}

@Composable
private fun IndividualModeContent(
    uiState: StopwatchUiState,
    upcomingAthletes: List<AthleteQueueItem>,
    onAction: (StopwatchAction) -> Unit,
    padding: PaddingValues
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Trial info
        uiState.currentAthlete?.let { athlete ->
            if (athlete.totalTrials > 1) {
                Text(
                    "Trial ${athlete.currentTrial} of ${athlete.totalTrials}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(Modifier.weight(0.1f))

        // Current athlete name
        Text(
            uiState.currentAthlete?.name ?: "",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.weight(0.15f))

        // Stopwatch display
        StopwatchDisplay(elapsedMs = uiState.elapsedMs)

        Spacer(Modifier.weight(0.15f))

        // Confirmation overlay
        AnimatedVisibility(
            visible = uiState.stopwatchPhase == StopwatchPhase.CONFIRMING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            uiState.confirmationData?.let { data ->
                ConfirmationFlash(data = data, canUndo = uiState.canUndo, onUndo = {
                    onAction(StopwatchAction.OnUndo)
                })
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(80.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.error
                    else PerformanceGreen
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    if (isRunning) "STOP" else "START",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.weight(0.1f))

        // Upcoming queue
        if (upcomingAthletes.isNotEmpty() && uiState.stopwatchPhase == StopwatchPhase.READY) {
            Text(
                "Up Next",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.3f)
                    .padding(horizontal = 24.dp)
            ) {
                items(upcomingAthletes) { athlete ->
                    AthleteQueueRow(athlete)
                }
            }
        } else {
            Spacer(Modifier.weight(0.3f))
        }
    }
}

@Composable
private fun GroupStartModeContent(
    uiState: StopwatchUiState,
    onAction: (StopwatchAction) -> Unit,
    padding: PaddingValues
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Heat info
        Text(
            "Heat ${uiState.currentHeatNumber} of ${uiState.totalHeats}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(16.dp))

        // Stopwatch display
        StopwatchDisplay(elapsedMs = uiState.elapsedMs)

        Spacer(Modifier.height(16.dp))

        // Confirmation overlay
        AnimatedVisibility(
            visible = uiState.stopwatchPhase == StopwatchPhase.CONFIRMING,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            uiState.confirmationData?.let { data ->
                ConfirmationFlash(data = data, canUndo = false, onUndo = {})
            }
        }

        // Action buttons
        if (uiState.stopwatchPhase != StopwatchPhase.CONFIRMING) {
            when (uiState.stopwatchPhase) {
                StopwatchPhase.READY -> {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAction(StopwatchAction.OnStartStop)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(80.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PerformanceGreen),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("START HEAT", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                }
                StopwatchPhase.RUNNING -> {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAction(StopwatchAction.OnFinishTap)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(80.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("TAP TO FINISH", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            onAction(StopwatchAction.OnStopHeat)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("STOP HEAT", fontWeight = FontWeight.Bold)
                    }
                }
                else -> {}
            }
        }

        Spacer(Modifier.height(16.dp))

        // Athlete list for this heat
        Text(
            "Athletes",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
        ) {
            items(uiState.heatAthletes) { athlete ->
                HeatAthleteRow(athlete)
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
    onUndo: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = PerformanceGreen.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = formatTimeDisplay(data.timeMs),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = PerformanceGreenText
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "saved for ${data.athleteName}",
                style = MaterialTheme.typography.bodyLarge,
                color = PerformanceGreenText
            )
            if (canUndo) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onUndo,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Undo")
                }
            }
        }
    }
}

@Composable
private fun AthleteQueueRow(athlete: AthleteQueueItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            athlete.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (athlete.totalTrials > 1) {
            Text(
                "Trial ${athlete.currentTrial}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeatAthleteRow(athlete: AthleteQueueItem) {
    val bgColor = when (athlete.status) {
        AthleteStatus.CAPTURED -> PerformanceGreen.copy(alpha = 0.1f)
        AthleteStatus.COMPLETED -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (athlete.status) {
            AthleteStatus.CAPTURED -> {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = PerformanceGreenText,
                    modifier = Modifier.size(20.dp)
                )
            }
            AthleteStatus.COMPLETED -> {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            else -> {
                Spacer(Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            athlete.name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        if (athlete.capturedTimeMs != null) {
            Text(
                formatTimeDisplay(athlete.capturedTimeMs),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                color = PerformanceGreenText
            )
        } else if (athlete.status == AthleteStatus.ACTIVE) {
            Text(
                "waiting...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatTimeDisplay(ms: Long): String {
    val minutes = (ms / 60000).toInt()
    val seconds = ((ms % 60000) / 1000).toInt()
    val centis = ((ms % 1000) / 10).toInt()
    return if (minutes > 0) {
        "%d:%02d.%02d".format(minutes, seconds, centis)
    } else {
        "%d.%02d".format(seconds, centis)
    }
}
