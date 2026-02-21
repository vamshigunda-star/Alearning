package com.example.alearning.ui.quicktest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.people.Individual

@Composable
fun QuickTestScreen(
    onNavigateBack: () -> Unit,
    viewModel: QuickTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    QuickTestContent(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                is QuickTestAction.OnNavigateBack -> onNavigateBack()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTestContent(
    uiState: QuickTestUiState,
    onAction: (QuickTestAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (uiState.step) {
                            QuickTestStep.SELECT_ATHLETE -> "Select Athlete"
                            QuickTestStep.SELECT_TESTS -> "Select Tests"
                            QuickTestStep.ENTER_SCORES -> "Enter Scores"
                            QuickTestStep.COMPLETE -> "Complete"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(QuickTestAction.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
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
            uiState.errorMessage != null && uiState.allAthletes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.errorMessage, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { onAction(QuickTestAction.OnDismissError) }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
            else -> {
                when (uiState.step) {
                    QuickTestStep.SELECT_ATHLETE -> SelectAthleteStep(uiState, onAction, padding)
                    QuickTestStep.SELECT_TESTS -> SelectTestsStep(uiState, onAction, padding)
                    QuickTestStep.ENTER_SCORES -> EnterScoresStep(uiState, onAction, padding)
                    QuickTestStep.COMPLETE -> CompleteStep(onAction, padding)
                }
            }
        }
    }
}

@Composable
private fun SelectAthleteStep(
    uiState: QuickTestUiState,
    onAction: (QuickTestAction) -> Unit,
    padding: PaddingValues
) {
    val filteredAthletes = if (uiState.searchQuery.isBlank()) {
        uiState.allAthletes
    } else {
        uiState.allAthletes.filter { athlete ->
            val query = uiState.searchQuery.lowercase()
            athlete.firstName.lowercase().contains(query) ||
                    athlete.lastName.lowercase().contains(query)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { onAction(QuickTestAction.OnSearchQueryChange(it)) },
            label = { Text("Search athletes") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )

        if (filteredAthletes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No athletes found. Register athletes first.")
            }
        } else {
            LazyColumn {
                items(filteredAthletes) { athlete ->
                    ListItem(
                        headlineContent = { Text("${athlete.firstName} ${athlete.lastName}") },
                        supportingContent = {
                            Text(
                                "Sex: ${athlete.sex.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier.clickable {
                            onAction(QuickTestAction.OnSelectAthlete(athlete))
                        }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun SelectTestsStep(
    uiState: QuickTestUiState,
    onAction: (QuickTestAction) -> Unit,
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Athlete info
        uiState.selectedAthlete?.let { athlete ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Text(
                        "${athlete.firstName} ${athlete.lastName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Category tabs
        if (uiState.categories.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = uiState.selectedCategoryIndex
            ) {
                uiState.categories.forEachIndexed { index, category ->
                    Tab(
                        selected = index == uiState.selectedCategoryIndex,
                        onClick = { onAction(QuickTestAction.OnSelectCategory(index)) },
                        text = { Text(category.name, maxLines = 1) }
                    )
                }
            }
        }

        // Test checkboxes
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(uiState.availableTests) { test ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(QuickTestAction.OnToggleTest(test.id)) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = test.id in uiState.selectedTestIds,
                        onCheckedChange = { onAction(QuickTestAction.OnToggleTest(test.id)) }
                    )
                    Column {
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

        // Confirm button
        Button(
            onClick = { onAction(QuickTestAction.OnConfirmTests) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            enabled = uiState.selectedTestIds.isNotEmpty()
        ) {
            Text("Continue with ${uiState.selectedTestIds.size} tests")
        }
    }
}

@Composable
private fun EnterScoresStep(
    uiState: QuickTestUiState,
    onAction: (QuickTestAction) -> Unit,
    padding: PaddingValues
) {
    val currentTest = uiState.selectedTests.getOrNull(uiState.currentTestIndex)
    var scoreText by remember(uiState.currentTestIndex) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress indicator
        LinearProgressIndicator(
            progress = { (uiState.currentTestIndex + 1).toFloat() / uiState.selectedTests.size },
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Test ${uiState.currentTestIndex + 1} of ${uiState.selectedTests.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Athlete name
        uiState.selectedAthlete?.let { athlete ->
            Text(
                "${athlete.firstName} ${athlete.lastName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (currentTest != null) {
            // Test info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        currentTest.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Unit: ${currentTest.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    currentTest.description?.let { desc ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(desc, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Score input
            OutlinedTextField(
                value = scoreText,
                onValueChange = { scoreText = it },
                label = { Text("Score (${currentTest.unit})") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.weight(1f))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onAction(QuickTestAction.OnSkipTest) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Skip")
                }
                Button(
                    onClick = {
                        scoreText.toDoubleOrNull()?.let { score ->
                            onAction(QuickTestAction.OnSaveScore(score))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = scoreText.toDoubleOrNull() != null && !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    } else {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
private fun CompleteStep(
    onAction: (QuickTestAction) -> Unit,
    padding: PaddingValues
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                "Testing Complete",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "All scores have been recorded.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = { onAction(QuickTestAction.OnNavigateBack) }) {
                Text("Back to Dashboard")
            }
        }
    }
}
