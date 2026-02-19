package com.example.alearning.ui.testing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun CreateEventScreen(
    onNavigateBack: () -> Unit,
    onEventCreated: (eventId: String, groupId: String) -> Unit,
    viewModel: CreateEventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle navigation event
    LaunchedEffect(uiState.eventCreated) {
        uiState.eventCreated?.let { (eventId, groupId) ->
            onEventCreated(eventId, groupId)
            viewModel.onAction(CreateEventAction.NavigationConsumed)
        }
    }

    CreateEventContent(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                is CreateEventAction.NavigateBack -> onNavigateBack()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventContent(
    uiState: CreateEventUiState,
    onAction: (CreateEventAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Testing Event") },
                navigationIcon = {
                    IconButton(onClick = { onAction(CreateEventAction.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { onAction(CreateEventAction.CreateEvent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    enabled = uiState.eventName.isNotBlank() &&
                            uiState.selectedGroupId != null &&
                            uiState.selectedTestIds.isNotEmpty() &&
                            !uiState.isCreating
                ) {
                    if (uiState.isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Text("Start Testing (${uiState.selectedTestIds.size} tests)")
                    }
                }
            }
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
            uiState.errorMessage != null && uiState.groups.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { onAction(CreateEventAction.ClearError) }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
            else -> {
                CreateEventBody(uiState = uiState, onAction = onAction, padding = padding)
            }
        }
    }

    // Inline error snackbar for validation errors
    if (uiState.errorMessage != null && uiState.groups.isNotEmpty()) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { onAction(CreateEventAction.ClearError) }) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(uiState.errorMessage)
        }
    }
}

@Composable
private fun CreateEventBody(
    uiState: CreateEventUiState,
    onAction: (CreateEventAction) -> Unit,
    padding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            OutlinedTextField(
                value = uiState.eventName,
                onValueChange = { onAction(CreateEventAction.SetEventName(it)) },
                label = { Text("Event Name") },
                placeholder = { Text("e.g., Fall Fitness Testing 2025") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Text("Select Group", style = MaterialTheme.typography.titleMedium)
        }
        items(uiState.groups) { group ->
            FilterChip(
                selected = group.id == uiState.selectedGroupId,
                onClick = { onAction(CreateEventAction.SelectGroup(group.id)) },
                label = { Text(group.name) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (uiState.categories.isNotEmpty()) {
            item {
                Text("Select Tests", style = MaterialTheme.typography.titleMedium)
            }

            item {
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTabIndex
                ) {
                    uiState.categories.forEachIndexed { index, category ->
                        Tab(
                            selected = index == uiState.selectedTabIndex,
                            onClick = { onAction(CreateEventAction.SelectTab(index)) },
                            text = { Text(category.name, maxLines = 1) }
                        )
                    }
                }
            }
        }

        items(uiState.availableTests) { test ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Checkbox(
                    checked = test.id in uiState.selectedTestIds,
                    onCheckedChange = { onAction(CreateEventAction.ToggleTest(test.id)) }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(test.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${test.unit} - ${if (test.isHigherBetter) "Higher is better" else "Lower is better"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
