package com.example.alearning.ui.testing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.standards.TestPreset
import com.example.alearning.ui.theme.*

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
                title = { 
                    Text(
                        "Create Testing Event", 
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(CreateEventAction.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = { onAction(CreateEventAction.CreateEvent) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    enabled = uiState.eventName.isNotBlank() &&
                            uiState.selectedGroupId != null &&
                            uiState.selectedTestIds.isNotEmpty() &&
                            !uiState.isCreating,
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0061FF) // Vibrant blue from image
                    )
                ) {
                    if (uiState.isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(
                            "Start Testing (${uiState.selectedTestIds.size} tests)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    ) { padding ->
        when {
            uiState.isLoading -> LoadingState()
            uiState.errorMessage != null && uiState.groups.isEmpty() -> ErrorState(
                message = uiState.errorMessage,
                onDismiss = { onAction(CreateEventAction.ClearError) }
            )
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

    if (uiState.isSavePresetDialogOpen) {
        SavePresetDialog(
            name = uiState.pendingPresetName,
            onNameChange = { onAction(CreateEventAction.SetPendingPresetName(it)) },
            onConfirm = { onAction(CreateEventAction.ConfirmSavePreset) },
            onDismiss = { onAction(CreateEventAction.DismissSavePresetDialog) }
        )
    }
}

@Composable
private fun PresetCard(
    preset: TestPreset,
    isApplied: Boolean,
    onApply: () -> Unit,
    onDelete: () -> Unit
) {
    val icon = when {
        preset.name.contains("Sprint", ignoreCase = true) -> Icons.AutoMirrored.Filled.DirectionsRun
        else -> Icons.Default.FitnessCenter
    }

    Surface(
        onClick = onApply,
        shape = RoundedCornerShape(16.dp),
        color = if (isApplied) Color(0xFFF0F6FF) else Color.White,
        border = BorderStroke(
            if (isApplied) 2.dp else 1.dp,
            if (isApplied) Color(0xFF0061FF) else Color(0xFFEEEEEE)
        ),
        modifier = Modifier
            .width(120.dp)
            .height(120.dp),
        shadowElevation = if (isApplied) 0.dp else 2.dp
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isApplied) Color.White else Color(0xFFF8F9FA),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFF0061FF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Column {
                    Text(
                        preset.name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        color = if (isApplied) Color(0xFF0061FF) else Color.Black
                    )
                    Text(
                        "${preset.testIds.size} Tests",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
            if (!preset.isBuiltIn) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 10.dp, y = (-10).dp)
                        .size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Delete",
                        modifier = Modifier.size(12.dp),
                        tint = Color.LightGray
                    )
                }
            }
        }
    }
}

@Composable
private fun SavePresetCard(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFF0061FF)),
        modifier = Modifier
            .width(120.dp)
            .height(120.dp),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF0F6FF),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF0061FF),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                "Save Current\nSelection",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                lineHeight = 14.sp,
                color = Color(0xFF0061FF)
            )
        }
    }
}

@Composable
private fun SavePresetDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save preset") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Give this test selection a name. You can apply it again later from Quick Select.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Preset name") },
                    placeholder = { Text("e.g., U12 Pre-Season") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateEventBody(
    uiState: CreateEventUiState,
    onAction: (CreateEventAction) -> Unit,
    padding: PaddingValues
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedGroup = uiState.groups.find { it.id == uiState.selectedGroupId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.eventName,
                    onValueChange = { onAction(CreateEventAction.SetEventName(it)) },
                    label = { Text("Event Name") },
                    placeholder = { Text("e.g. Morning Sprints") },
                    modifier = Modifier.weight(1.1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF0061FF),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedLabelColor = Color(0xFF0061FF)
                    )
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.weight(0.9f)
                ) {
                    OutlinedTextField(
                        value = selectedGroup?.name ?: "",
                        onValueChange = {},
                        label = { Text("Group") },
                        placeholder = { Text("Select") },
                        readOnly = true,
                        trailingIcon = { 
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0061FF),
                            unfocusedBorderColor = Color(0xFFE0E0E0),
                            focusedLabelColor = Color(0xFF0061FF)
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        uiState.groups.forEach { group ->
                            DropdownMenuItem(
                                text = { Text(group.name) },
                                onClick = {
                                    onAction(CreateEventAction.SelectGroup(group.id))
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Quick Select",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SavePresetCard(
                    enabled = uiState.selectedTestIds.isNotEmpty(),
                    onClick = { onAction(CreateEventAction.OpenSavePresetDialog) }
                )
                uiState.presets.forEach { preset ->
                    val applied = preset.testIds.isNotEmpty() &&
                            uiState.selectedTestIds == preset.testIds.toSet()
                    PresetCard(
                        preset = preset,
                        isApplied = applied,
                        onApply = { onAction(CreateEventAction.ApplyPreset(preset.id)) },
                        onDelete = { onAction(CreateEventAction.DeletePreset(preset.id)) }
                    )
                }
            }
        }

        if (uiState.categories.isNotEmpty()) {
            item {
                Text(
                    "Select Tests", 
                    style = MaterialTheme.typography.titleLarge, 
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                ScrollableTabRow(
                    selectedTabIndex = uiState.selectedTabIndex,
                    edgePadding = 0.dp,
                    containerColor = Color.Transparent,
                    divider = {},
                    indicator = { tabPositions ->
                        if (uiState.selectedTabIndex < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTabIndex]),
                                color = Color(0xFF0061FF)
                            )
                        }
                    }
                ) {
                    uiState.categories.forEachIndexed { index, category ->
                        Tab(
                            selected = index == uiState.selectedTabIndex,
                            onClick = { onAction(CreateEventAction.SelectTab(index)) },
                            text = { 
                                Text(
                                    category.name, 
                                    maxLines = 1, 
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (index == uiState.selectedTabIndex) Color.Black else Color.Gray
                                ) 
                            }
                        )
                    }
                }
            }
        }

        items(uiState.availableTests) { test ->
            val isSelected = test.id in uiState.selectedTestIds
            Surface(
                onClick = { onAction(CreateEventAction.ToggleTest(test.id)) },
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onAction(CreateEventAction.ToggleTest(test.id)) },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF0061FF))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            test.name, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${test.unit} \u00b7 ${if (test.isHigherBetter) "Higher is better" else "Lower is better"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
