package com.vamshi.field.ui.testing

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
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
import com.vamshi.field.domain.model.standards.TestPreset
import com.vamshi.field.ui.components.AppTopBar
import com.vamshi.field.ui.theme.*

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
            AppTopBar(
                title = "Create Testing Event",
                navigationIcon = {
                    IconButton(onClick = { onAction(CreateEventAction.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
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
                        containerColor = MaterialTheme.colorScheme.primary // Vibrant blue from image
                    )
                ) {
                    if (uiState.isCreating) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
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
            uiState.errorMessage != null && uiState.groups.isEmpty() -> InternalErrorState(
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
        color = if (isApplied) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (isApplied) 2.dp else 1.dp,
            if (isApplied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
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
                    color = if (isApplied) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
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
                        color = if (isApplied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${preset.testIds.size} Tests",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
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
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                "Save Current\nSelection",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                lineHeight = 14.sp,
                color = MaterialTheme.colorScheme.primary
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
private fun InternalErrorState(
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
    var groupDropdownExpanded by remember { mutableStateOf(false) }
    var quickSelectExpanded by remember { mutableStateOf(false) }
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExposedDropdownMenuBox(
                    expanded = groupDropdownExpanded,
                    onExpandedChange = { groupDropdownExpanded = !groupDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val groupText = selectedGroup?.let {
                        val count = uiState.groupAthleteCounts[it.id] ?: 0
                        "${it.name} • $count"
                    } ?: ""
                    
                    OutlinedTextField(
                        value = groupText,
                        onValueChange = {},
                        label = { Text("Group") },
                        singleLine = true,
                        leadingIcon = { 
                            Icon(Icons.Default.Person, contentDescription = "Group", modifier = Modifier.size(16.dp))
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold),
                        placeholder = { Text("Select") },
                        readOnly = true,
                        trailingIcon = { 
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = groupDropdownExpanded,
                        onDismissRequest = { groupDropdownExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        uiState.groups.forEach { group ->
                            val count = uiState.groupAthleteCounts[group.id] ?: 0
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = group.name,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("$count", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    onAction(CreateEventAction.SelectGroup(group.id))
                                    groupDropdownExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.eventName,
                    onValueChange = { onAction(CreateEventAction.SetEventName(it)) },
                    label = { Text("Event Name") },
                    leadingIcon = {
                        Icon(Icons.Default.Event, contentDescription = "Event Name", modifier = Modifier.size(16.dp))
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(fontWeight = FontWeight.Bold),
                    placeholder = { Text("e.g. Morning Sprints") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
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
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    uiState.categories.forEachIndexed { index, category ->
                        val testCount = uiState.allTests.count { it.categoryId == category.id }
                        Tab(
                            selected = index == uiState.selectedTabIndex,
                            onClick = { onAction(CreateEventAction.SelectTab(index)) },
                            text = { 
                                val isSelected = index == uiState.selectedTabIndex
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                    shadowElevation = if (isSelected) 2.dp else 0.dp,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            category.name, 
                                            maxLines = 1, 
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Surface(
                                            shape = androidx.compose.foundation.shape.CircleShape,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    "$testCount",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
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
                color = MaterialTheme.colorScheme.surface,
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
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            test.name, 
                            style = MaterialTheme.typography.titleMedium, 
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                test.unit,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val badgeColor = if (test.isHigherBetter) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            val textColor = if (test.isHigherBetter) Color(0xFF2E7D32) else Color(0xFFC62828)
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = badgeColor
                            ) {
                                Text(
                                    if (test.isHigherBetter) "Higher is Better" else "Lower is Better",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Quick Select",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            ExposedDropdownMenuBox(
                expanded = quickSelectExpanded,
                onExpandedChange = { quickSelectExpanded = !quickSelectExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                // Determine current selection text
                val matchedPreset = uiState.presets.find { preset -> 
                    preset.testIds.isNotEmpty() && uiState.selectedTestIds == preset.testIds.toSet() 
                }
                val selectionText = matchedPreset?.name ?: "Current Selection (${uiState.selectedTestIds.size} tests)"
                
                OutlinedTextField(
                    value = selectionText,
                    onValueChange = {},
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = quickSelectExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                ExposedDropdownMenu(
                    expanded = quickSelectExpanded,
                    onDismissRequest = { quickSelectExpanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text("Current Selection (${uiState.selectedTestIds.size} tests)") },
                        onClick = { quickSelectExpanded = false }
                    )
                    
                    if (uiState.presets.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        
                        uiState.presets.forEach { preset ->
                            val isApplied = preset.testIds.isNotEmpty() && uiState.selectedTestIds == preset.testIds.toSet()
                            DropdownMenuItem(
                                text = { 
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            preset.name,
                                            fontWeight = if (isApplied) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isApplied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (!preset.isBuiltIn) {
                                            IconButton(
                                                onClick = {
                                                    onAction(CreateEventAction.DeletePreset(preset.id))
                                                    quickSelectExpanded = false
                                                },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Delete Preset",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    onAction(CreateEventAction.ApplyPreset(preset.id))
                                    quickSelectExpanded = false
                                }
                            )
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    DropdownMenuItem(
                        text = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Save Current Selection as Preset", color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        onClick = {
                            onAction(CreateEventAction.OpenSavePresetDialog)
                            quickSelectExpanded = false
                        }
                    )
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
