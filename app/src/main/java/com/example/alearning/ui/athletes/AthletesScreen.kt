package com.example.alearning.ui.athletes

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.ui.components.AppTopBar
import com.example.alearning.ui.roster.AthleteTabContent
import com.example.alearning.ui.roster.RosterAction
import com.example.alearning.ui.roster.RosterDialogs
import com.example.alearning.ui.roster.RosterViewModel
import com.example.alearning.ui.theme.SportOrange

@Composable
fun AthletesScreen(
    onNavigateToAthleteReport: (String) -> Unit,
    viewModel: RosterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Athletes",
                actions = {
                    IconButton(onClick = { viewModel.onAction(RosterAction.OnShowRegisterAthleteDialog) }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Register Athlete")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAction(RosterAction.OnShowRegisterAthleteDialog) },
                containerColor = SportOrange,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Athlete")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            AthleteTabContent(
                uiState = uiState,
                onAction = { action ->
                    when (action) {
                        is RosterAction.OnNavigateToAthleteReport -> onNavigateToAthleteReport(action.individualId)
                        else -> viewModel.onAction(action)
                    }
                }
            )
        }
    }

    RosterDialogs(uiState = uiState, onAction = { action ->
        when (action) {
            is RosterAction.OnNavigateToAthleteReport -> onNavigateToAthleteReport(action.individualId)
            else -> viewModel.onAction(action)
        }
    })
}
