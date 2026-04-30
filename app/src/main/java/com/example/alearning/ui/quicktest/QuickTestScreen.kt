package com.example.alearning.ui.quicktest

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.ui.theme.*
import com.example.alearning.ui.components.testing.PercentileGauge
import com.example.alearning.ui.components.testing.TestInputSwitcher
import com.example.alearning.domain.model.people.BiologicalSex

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
        },
        onLiveScoreChange = { viewModel.onLiveScoreChange(it) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickTestContent(
    uiState: QuickTestUiState,
    onAction: (QuickTestAction) -> Unit,
    onLiveScoreChange: (Double) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        when (uiState.step) {
                            QuickTestStep.SETUP -> "Quick Test Setup"
                            QuickTestStep.ENTER_SCORES -> "Enter Scores"
                            QuickTestStep.COMPLETE -> "Complete"
                        },
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(QuickTestAction.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (uiState.step == QuickTestStep.SETUP) {
                Surface(tonalElevation = 3.dp) {
                    val canStart = uiState.athleteSearchQuery.isNotBlank() && 
                                 uiState.selectedTestIds.isNotEmpty() &&
                                 (!uiState.isGuest || (uiState.guestAge.isNotBlank() && uiState.guestSex != BiologicalSex.UNSPECIFIED))

                    Button(
                        onClick = { onAction(QuickTestAction.OnConfirmSetup) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        enabled = canStart,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Start Testing", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState()
            uiState.errorMessage != null && uiState.categories.isEmpty() -> ErrorState(
                message = uiState.errorMessage,
                onDismiss = { onAction(QuickTestAction.OnDismissError) }
            )
            else -> {
                Box(modifier = Modifier.padding(padding)) {
                    when (uiState.step) {
                        QuickTestStep.SETUP -> SetupStep(uiState, onAction)
                        QuickTestStep.ENTER_SCORES -> EnterScoresStep(uiState, onAction, onLiveScoreChange)
                        QuickTestStep.COMPLETE -> CompleteStep(uiState, onAction)
                    }

                    // Floating error message if any
                    if (uiState.errorMessage != null) {
                        Snackbar(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                            action = {
                                TextButton(onClick = { onAction(QuickTestAction.OnDismissError) }) {
                                    Text("Dismiss")
                                }
                            }
                        ) {
                            Text(uiState.errorMessage)
                        }
                    }
                }
            }
        }
    }

    if (uiState.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { onAction(QuickTestAction.OnDismissDelete) },
            title = { Text("Delete Recorded Scores?") },
            text = { Text("This will permanently remove all scores recorded in this quick test session. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { onAction(QuickTestAction.OnConfirmDelete) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(QuickTestAction.OnDismissDelete) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupStep(
    uiState: QuickTestUiState,
    onAction: (QuickTestAction) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Athlete Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = uiState.athleteSearchQuery,
                    onValueChange = { onAction(QuickTestAction.OnAthleteQueryChange(it)) },
                    label = { Text("Athlete Name") },
                    placeholder = { Text("Enter name or search...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    trailingIcon = {
                        if (uiState.selectedAthlete != null) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                        }
                    },
                    shape = MaterialTheme.shapes.medium
                )

                // Search Suggestions
                if (uiState.matchingAthletes.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        tonalElevation = 2.dp,
                        shadowElevation = 4.dp
                    ) {
                        Column {
                            uiState.matchingAthletes.take(3).forEach { athlete ->
                                ListItem(
                                    headlineContent = { Text(athlete.fullName) },
                                    supportingContent = { Text(athlete.sex.name) },
                                    leadingContent = { 
                                        Box(Modifier.size(32.dp).background(NavyPrimary, CircleShape), contentAlignment = Alignment.Center) {
                                            Text(athlete.firstName.first().toString(), color = Color.White)
                                        }
                                    },
                                    modifier = Modifier.clickable { onAction(QuickTestAction.OnSelectAthlete(athlete)) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (uiState.isGuest && uiState.athleteSearchQuery.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Guest Details (Required for Percentiles)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = uiState.guestAge,
                                onValueChange = { if (it.all { char -> char.isDigit() }) onAction(QuickTestAction.OnSetGuestAge(it)) },
                                label = { Text("Age") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            
                            var expanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = uiState.guestSex.name,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Sex") },
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = {ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent
                                    ),
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                        .also { interactionSource ->
                                            LaunchedEffect(interactionSource) {
                                                interactionSource.interactions.collect {
                                                    if (it is androidx.compose.foundation.interaction.PressInteraction.Release) {
                                                        expanded = !expanded
                                                    }
                                                }
                                            }
                                        }
                                )
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    BiologicalSex.values().filter { it != BiologicalSex.UNSPECIFIED }.forEach { sex ->
                                        DropdownMenuItem(
                                            text = { Text(sex.name) },
                                            onClick = {
                                                onAction(QuickTestAction.OnSetGuestSex(sex))
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        item {
            OutlinedTextField(
                value = uiState.eventName,
                onValueChange = { onAction(QuickTestAction.OnSetEventName(it)) },
                label = { Text("Event Name (Optional)") },
                placeholder = { Text("e.g., Morning Session") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium
            )
        }

        item {
            Text(
                "Select Tests",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            if (uiState.categories.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedCategoryIndex,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {}
                ) {
                    uiState.categories.forEachIndexed { index, category ->
                        Tab(
                            selected = index == uiState.selectedCategoryIndex,
                            onClick = { onAction(QuickTestAction.OnSelectCategory(index)) },
                            text = { Text(category.name, style = MaterialTheme.typography.labelLarge) }
                        )
                    }
                }
            }
        }

        items(uiState.availableTests) { test ->
            val isSelected = test.id in uiState.selectedTestIds
            OutlinedCard(
                onClick = { onAction(QuickTestAction.OnToggleTest(test.id)) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = SolidColor(MaterialTheme.colorScheme.primary))
                else CardDefaults.outlinedCardBorder()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onAction(QuickTestAction.OnToggleTest(test.id)) }
                    )
                    Column {
                        Text(test.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${test.unit} \u00b7 ${if (test.isHigherBetter) "Higher is better" else "Lower is better"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnterScoresStep(
    uiState: QuickTestUiState,
    onAction: (QuickTestAction) -> Unit,
    onLiveScoreChange: (Double) -> Unit
) {
    val currentTest = uiState.selectedTests.getOrNull(uiState.currentTestIndex)
    var scoreText by remember(uiState.currentTestIndex) { mutableStateOf("") }

    val validMin = currentTest?.validMin
    val validMax = currentTest?.validMax
    val isInRange = remember(scoreText, validMin, validMax) {
        val v = scoreText.toDoubleOrNull() ?: return@remember false
        val minOk = validMin?.let { v >= it } ?: true
        val maxOk = validMax?.let { v <= it } ?: true
        minOk && maxOk
    }

    // Trigger live calculation whenever text changes
    LaunchedEffect(scoreText) {
        scoreText.toDoubleOrNull()?.let { onLiveScoreChange(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Progress indicator
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LinearProgressIndicator(
                progress = { (uiState.currentTestIndex + 1).toFloat() / uiState.selectedTests.size },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(MaterialTheme.shapes.small)
            )
            Text(
                "Test ${uiState.currentTestIndex + 1} of ${uiState.selectedTests.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End
            )
        }

        // Athlete info
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (uiState.isGuest) Color(0xFF546E7A) else NavyPrimary
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White)
                Column {
                    Text(
                        if (uiState.isGuest) "${uiState.athleteSearchQuery} (Guest)" 
                        else uiState.selectedAthlete?.fullName ?: "Unknown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (uiState.isGuest) {
                        Text(
                            "Guest: ${uiState.guestSex}, Age ${uiState.guestAge}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        if (currentTest != null) {
            // Live Percentile Gauge (Compact)
            PercentileGauge(
                percentile = uiState.lastRecordedPercentile,
                modifier = Modifier.fillMaxWidth().height(140.dp)
            )

            // Test info & Score Display (Combined for space)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            currentTest.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Text("Unit: ${currentTest.unit}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Surface(
                    modifier = Modifier.width(120.dp).height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = scoreText.ifEmpty { "0.0" },
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (scoreText.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (!isInRange && scoreText.isNotEmpty()) {
                Text(
                    text = buildString {
                        append("Valid range: ")
                        if (validMin != null) append("≥ $validMin")
                        if (validMin != null && validMax != null) append(" and ")
                        if (validMax != null) append("≤ $validMax")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            // DYNAMIC MODULAR INPUT
            TestInputSwitcher(
                paradigm = currentTest.inputParadigm,
                currentValue = scoreText,
                onValueChange = { scoreText = it },
                onSubmit = {
                    if (isInRange) {
                        scoreText.toDoubleOrNull()?.let { score ->
                            onAction(QuickTestAction.OnSaveScore(score))
                        }
                    }
                }
            )

            // Skip Action
            TextButton(
                onClick = { onAction(QuickTestAction.OnSkipTest) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.SkipNext, null)
                Spacer(Modifier.width(8.dp))
                Text("Skip this test")
            }
        }
    }
}

@Composable
private fun CompleteStep(
    uiState: QuickTestUiState,
    onAction: (QuickTestAction) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(80.dp), tint = PerformanceGreen)
        Spacer(Modifier.height(24.dp))
        Text("Quick Test Complete", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            if (uiState.isGuest) "Scores have been calculated for the guest athlete."
            else "All scores have been recorded to the athlete's history.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(48.dp))
        
        Button(
            onClick = { onAction(QuickTestAction.OnNavigateBack) },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Text("Back to Dashboard", style = MaterialTheme.typography.titleMedium)
        }
        
        if (!uiState.isGuest) {
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = { onAction(QuickTestAction.OnRequestDelete) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Delete These Results", style = MaterialTheme.typography.titleMedium)
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
            CircularProgressIndicator(strokeWidth = 3.dp)
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ErrorState(message: String, onDismiss: () -> Unit) {
    EmptyState(
        icon = Icons.Default.ErrorOutline,
        title = "An error occurred",
        message = message,
        actionLabel = "Dismiss",
        onAction = onDismiss
    )
}

@Composable
private fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}
