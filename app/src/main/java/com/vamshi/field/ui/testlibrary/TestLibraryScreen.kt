package com.vamshi.field.ui.testlibrary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vamshi.field.domain.model.standards.FitnessTest
import com.vamshi.field.ui.components.AppTopBar
import com.vamshi.field.ui.components.CategoryDescription
import com.vamshi.field.ui.components.CategorySelector
import com.vamshi.field.ui.components.video.TestVideoPreview

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
            AppTopBar(
                title = "Test Library",
                navigationIcon = {
                    IconButton(onClick = { onAction(TestLibraryAction.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState()
            uiState.errorMessage != null -> ErrorState(
                message = uiState.errorMessage,
                onDismiss = { onAction(TestLibraryAction.OnDismissError) }
            )
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
private fun LoadingState(message: String = "Loading...") {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorState(
    message: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun TestLibraryBody(
    uiState: TestLibraryUiState,
    onAction: (TestLibraryAction) -> Unit,
    padding: PaddingValues
) {
    // NavigableListDetailPaneScaffold requires a ThreePaneScaffoldNavigator<Any>; the
    // destination content is always a test id (String) — see the detail pane read below.
    val navigator = rememberListDetailPaneScaffoldNavigator<Any>()

    BackHandler(navigator.canNavigateBack()) {
        navigator.navigateBack()
    }

    NavigableListDetailPaneScaffold(
        modifier = Modifier.padding(padding),
        navigator = navigator,
        listPane = {
            AnimatedPane {
                Column(modifier = Modifier.fillMaxSize()) {
                    CategorySelector(
                        categories = uiState.categories,
                        selected = uiState.selectedCategory,
                        onSelect = { category ->
                            onAction(TestLibraryAction.OnSelectCategory(category))
                            if (navigator.canNavigateBack()) {
                                navigator.navigateBack()
                            }
                        },
                        label = { it.name },
                        key = { it.id }
                    )

                    CategoryDescription(
                        description = uiState.selectedCategory?.description
                    )

                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { onAction(TestLibraryAction.OnSearchQueryChanged(it)) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search tests...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        singleLine = true
                    )

                    val filteredTests = remember(uiState.testsForCategory, uiState.searchQuery) {
                        uiState.filteredTests
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(filteredTests, key = { it.id }) { test ->
                            TestListCard(
                                test = test,
                                categoryName = uiState.selectedCategory?.name ?: "",
                                onClick = { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, test.id) }
                            )
                        }
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                val testId = navigator.currentDestination?.content as? String
                val selectedTest = remember(testId, uiState.testsForCategory) {
                    uiState.testsForCategory.find { it.id == testId }
                }
                if (selectedTest != null) {
                    TestDetailPane(test = selectedTest)
                } else {
                    EmptyDetailPane()
                }
            }
        }
    )
}

@Composable
private fun TestListCard(test: FitnessTest, categoryName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            TestVideoPreview(
                youtubeId = test.youtubeId,
                testName = test.name
            )

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    test.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$categoryName • ${test.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Trend/Context Indicator
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (test.isHigherBetter) Icons.AutoMirrored.Filled.TrendingUp
                            else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            if (test.isHigherBetter) "Higher is better" else "Lower is better",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    TextButton(
                        onClick = onClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                    ) {
                        Text("View Test")
                    }
                }
            }
        }
    }
}

@Composable
private fun TestDetailPane(test: FitnessTest) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = test.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        test.description?.let { desc ->
            Text(
                text = desc,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        test.youtubeId?.let { youtubeId ->
            TestVideoPreview(
                youtubeId = youtubeId,
                testName = test.name,
                height = 200.dp,
                cornerShape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
private fun EmptyDetailPane() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Select a test to view details", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
