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

val PendingTint = Color(0xFFFFF3CD)
val PendingBorder = Color(0xFFE65100)

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
            Log.d("TestingGridScreen", "onAction triggered: $action")
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

    uiState.timingChoiceCell?.let { choice ->
        TimingChoiceDialog(
            athleteName = "${choice.athlete.firstName} ${choice.athlete.lastName}",
            testName = choice.test.name,
            unit = choice.test.unit,
            onUseStopwatch = {
                viewModel.onAction(TestingGridAction.OnSelectTimingMethod(choice.test.id, CaptureMethodPreference.STOPWATCH))
                viewModel.onAction(TestingGridAction.OnDismissTimingChoice)
                onNavigateToStopwatch(viewModel.eventId, choice.test.id, viewModel.groupId, choice.athlete.id)
            },
            onEnterManually = {
                viewModel.onAction(TestingGridAction.OnSelectTimingMethod(choice.test.id, CaptureMethodPreference.MANUAL))
                viewModel.onAction(TestingGridAction.OnDismissTimingChoice)
                viewModel.onAction(TestingGridAction.OnStartEditing(choice.athlete, choice.test))
            },
            onDismiss = { viewModel.onAction(TestingGridAction.OnDismissTimingChoice) }
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
            AppTopBar(
                title = {
                    when (uiState.phase) {
                        TestingPhase.LIVE_ENTRY -> Column {
                            Text("Live Testing", style = MaterialTheme.typography.titleLarge)
                            Text(sessionTimeStr, style = MaterialTheme.typography.labelSmall, color = AppTopBarSubtitleColor)
                        }
                        TestingPhase.EVENT_DETAIL -> Text("Testing Event", style = MaterialTheme.typography.titleLarge)
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
                }
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

