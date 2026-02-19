package com.example.alearning.ui.testlibrary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.standards.FitnessTest

@Composable
fun TestLibraryScreen(
    onNavigateBack: () -> Unit,
    viewModel: TestLibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    TestLibraryContent(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                is TestLibraryAction.OnNavigateBack -> onNavigateBack()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestLibraryContent(
    uiState: TestLibraryUiState,
    onAction: (TestLibraryAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Test Library") },
                navigationIcon = {
                    IconButton(onClick = { onAction(TestLibraryAction.OnNavigateBack) }) {
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
            uiState.errorMessage != null -> {
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
                        TextButton(onClick = { onAction(TestLibraryAction.OnDismissError) }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
            uiState.categories.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No fitness tests available")
                }
            }
            else -> {
                TestLibraryBody(uiState = uiState, onAction = onAction, padding = padding)
            }
        }
    }
}

@Composable
private fun TestLibraryBody(
    uiState: TestLibraryUiState,
    onAction: (TestLibraryAction) -> Unit,
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        ScrollableTabRow(
            selectedTabIndex = uiState.categories.indexOf(uiState.selectedCategory).coerceAtLeast(0)
        ) {
            uiState.categories.forEach { category ->
                Tab(
                    selected = category == uiState.selectedCategory,
                    onClick = { onAction(TestLibraryAction.OnSelectCategory(category)) },
                    text = { Text(category.name, maxLines = 1) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.testsForCategory) { test ->
                TestCard(test = test)
            }
        }
    }
}

@Composable
private fun TestCard(test: FitnessTest) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    test.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = { Text(test.unit) },
                    leadingIcon = {
                        Icon(
                            if (test.isHigherBetter) Icons.AutoMirrored.Filled.TrendingUp
                            else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = if (test.isHigherBetter) "Higher is better" else "Lower is better",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            test.description?.let { desc ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
