package com.example.alearning.ui.roster

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.model.people.Group
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.ui.components.AppTopBar
import com.example.alearning.ui.components.RegisterAthleteSheet
import com.example.alearning.ui.theme.SportOrange
import com.example.alearning.ui.theme.SportOrangeContainer
import java.util.*

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.activity.compose.BackHandler
import com.example.alearning.ui.athlete.AthleteDashboardScreen

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun RosterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAthleteReport: (String) -> Unit, // no longer invoked for athlete clicks
    onNavigateToTest: (String, String, String?) -> Unit,
    viewModel: RosterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val navigator = rememberListDetailPaneScaffoldNavigator<Any>()

    BackHandler(navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                RosterContent(
                    uiState = uiState,
                    onAction = { action ->
                        when (action) {
                            is RosterAction.OnNavigateBack -> {
                                if (navigator.canNavigateBack()) {
                                    navigator.navigateBack()
                                } else {
                                    onNavigateBack()
                                }
                            }
                            is RosterAction.OnNavigateToAthleteReport ->
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, action.individualId as Any)
                            else -> viewModel.onAction(action)
                        }
                    }
                )

                RosterDialogs(uiState = uiState, onAction = { action ->
                    when (action) {
                        is RosterAction.OnNavigateToAthleteReport ->
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, action.individualId as Any)
                        else -> viewModel.onAction(action)
                    }
                })
            }
        },
        detailPane = {
            AnimatedPane {
                val athleteId = navigator.currentDestination?.content as? String
                if (athleteId != null) {
                    AthleteDashboardScreen(
                        athleteId = athleteId,
                        contextSessionId = null,
                        onNavigateBack = { navigator.navigateBack() },
                        onNavigateToTest = onNavigateToTest,
                        onStartQuickTest = { _, _ -> /* no-op in detail pane or handle if needed */ },
                        onNavigateToAiCoach = {}
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Select an item to view details", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterContent(
    uiState: RosterUiState,
    onAction: (RosterAction) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Roster",
                navigationIcon = {
                    IconButton(onClick = { onAction(RosterAction.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(RosterAction.OnShowRegisterAthleteDialog) }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Register Athlete")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.selectedAthleteIds.isEmpty()) {
                FloatingActionButton(
                    onClick = {
                        if (uiState.currentTab == RosterTab.ATHLETES) {
                            onAction(RosterAction.OnShowRegisterAthleteDialog)
                        } else {
                            onAction(RosterAction.OnShowAddGroupDialog)
                        }
                    },
                    containerColor = SportOrange,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            RosterTabRow(
                currentTab = uiState.currentTab,
                onTabSelected = { onAction(RosterAction.OnTabSelected(it)) },
                athleteCount = uiState.allAthletes.size,
                groupCount = uiState.groups.size
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> LoadingState()
                    uiState.errorMessage != null && uiState.allAthletes.isEmpty() -> ErrorState(
                        message = uiState.errorMessage,
                        onDismiss = { onAction(RosterAction.OnDismissError) }
                    )
                    uiState.currentTab == RosterTab.ATHLETES -> AthleteTabContent(uiState, onAction)
                    uiState.currentTab == RosterTab.GROUPS -> GroupsTabContent(uiState, onAction)
                }
                
                // Contextual Action Bar
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.selectedAthleteIds.isNotEmpty(),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                ) {
                    ContextualActionBar(
                        selectedCount = uiState.selectedAthleteIds.size,
                        onAddToGroup = { onAction(RosterAction.OnShowAddToGroupDialog) }
                    )
                }
            }
        }
    }

    // Floating error message
    if (uiState.errorMessage != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { onAction(RosterAction.OnDismissError) }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(uiState.errorMessage)
        }
    }
}

