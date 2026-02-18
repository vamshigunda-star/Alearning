package com.example.alearning.ui.testing

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventScreen(
    onNavigateBack: () -> Unit,
    onEventCreated: (eventId: String, groupId: String) -> Unit,
    viewModel: CreateEventViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Testing Event") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { viewModel.createEvent(onEventCreated) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Event name
            item {
                OutlinedTextField(
                    value = uiState.eventName,
                    onValueChange = { viewModel.setEventName(it) },
                    label = { Text("Event Name") },
                    placeholder = { Text("e.g., Fall Fitness Testing 2025") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Group selection
            item {
                Text("Select Group", style = MaterialTheme.typography.titleMedium)
            }
            items(uiState.groups) { group ->
                FilterChip(
                    selected = group.id == uiState.selectedGroupId,
                    onClick = { viewModel.selectGroup(group.id) },
                    label = { Text(group.name) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category tabs for test selection
            if (uiState.categories.isNotEmpty()) {
                item {
                    Text("Select Tests", style = MaterialTheme.typography.titleMedium)
                }

                item {
                    ScrollableTabRow(
                        selectedTabIndex = 0
                    ) {
                        uiState.categories.forEachIndexed { _, category ->
                            Tab(
                                selected = false,
                                onClick = { viewModel.loadTestsForCategory(category.id) },
                                text = { Text(category.name, maxLines = 1) }
                            )
                        }
                    }
                }
            }

            // Test checkboxes
            items(uiState.availableTests) { test ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = test.id in uiState.selectedTestIds,
                        onCheckedChange = { viewModel.toggleTest(test.id) }
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
}
