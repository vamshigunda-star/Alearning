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
import java.util.*

// Modern minimalist color palette
private val BackgroundGray = Color(0xFFF8F9FB)
private val SurfaceWhite = Color(0xFFFFFFFF)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF757575)
private val BrandAccent = Color(0xFFF97D28) // Your orange accent
private val BorderLight = Color(0xFFE5E7EB)
private val AvatarBackground = Color(0xFFFFF4EC)

@Composable
fun RosterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAthleteReport: (String) -> Unit,
    viewModel: RosterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    RosterContent(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                is RosterAction.OnNavigateBack -> onNavigateBack()
                is RosterAction.OnNavigateToAthleteReport -> onNavigateToAthleteReport(action.individualId)
                else -> viewModel.onAction(action)
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
        containerColor = BackgroundGray,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Roster Manager",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(RosterAction.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // Modern Header Actions: Group and Athlete
                    IconButton(onClick = { onAction(RosterAction.OnShowAddGroupDialog) }) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "Create Group", tint = BrandAccent)
                    }
                    IconButton(onClick = { onAction(RosterAction.OnShowRegisterAthleteDialog) }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Register Athlete", tint = BrandAccent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite,
                    scrolledContainerColor = SurfaceWhite
                ),
                modifier = Modifier.border(width = 1.dp, color = BorderLight, shape = RoundedCornerShape(0.dp))
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.errorMessage != null -> ErrorState(
                    message = uiState.errorMessage,
                    onDismiss = { onAction(RosterAction.OnDismissError) }
                )
                else -> RosterBody(uiState = uiState, onAction = onAction)
            }
        }
    }

    // Modern Bottom Sheets
    if (uiState.showAddGroupDialog) {
        AddGroupSheet(
            onDismiss = { onAction(RosterAction.OnDismissAddGroupDialog) },
            onConfirm = { name, location, cycle -> onAction(RosterAction.OnCreateGroup(name, location, cycle)) }
        )
    }

    if (uiState.showRegisterAthleteDialog) {
        RegisterAthleteSheet(
            onDismiss = { onAction(RosterAction.OnDismissRegisterAthleteDialog) },
            onConfirm = { first, last, dob, sex, email, med -> onAction(RosterAction.OnRegisterAthlete(first, last, dob, sex, email, med)) }
        )
    }

    if (uiState.showAddToGroupDialog) {
        ManageGroupMembersSheet(
            allAthletes = uiState.allAthletes,
            currentAthleteIds = uiState.athletesInGroup.map { it.id }.toSet(),
            onDismiss = { onAction(RosterAction.OnDismissAddToGroupDialog) },
            onAddAthlete = { id -> onAction(RosterAction.OnAddAthleteToGroup(id)) },
            onRemoveAthlete = { id -> onAction(RosterAction.OnRemoveAthleteFromGroup(id)) }
        )
    }

    // Floating error message
    if (uiState.errorMessage != null && uiState.groups.isNotEmpty()) {
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

@Composable
private fun RosterBody(
    uiState: RosterUiState,
    onAction: (RosterAction) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (uiState.groups.isEmpty()) {
            EmptyGroupState(onCreateGroup = { onAction(RosterAction.OnShowAddGroupDialog) })
        } else {
            // Sleek Group Selector
            Surface(
                color = SurfaceWhite,
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 1.dp
            ) {
                GroupSelectorRow(
                    groups = uiState.groups,
                    selectedGroup = uiState.selectedGroup,
                    onSelectGroup = { onAction(RosterAction.OnSelectGroup(it)) },
                    onCreateGroup = { onAction(RosterAction.OnShowAddGroupDialog) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState.selectedGroup != null) {
                // List Header Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${uiState.athletesInGroup.size} Athletes",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    TextButton(
                        onClick = { onAction(RosterAction.OnShowAddToGroupDialog) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandAccent)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add to Group", color = BrandAccent, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Athlete List
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.athletesInGroup, key = { it.id }) { athlete ->
                        ModernAthleteRow(
                            athlete = athlete,
                            onTap = { onAction(RosterAction.OnNavigateToAthleteReport(athlete.id)) },
                            onRemove = { onAction(RosterAction.OnRemoveAthleteFromGroup(athlete.id)) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun GroupSelectorRow(
    groups: List<Group>,
    selectedGroup: Group?,
    onSelectGroup: (Group) -> Unit,
    onCreateGroup: () -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(groups, key = { it.id }) { group ->
            val isSelected = group.id == selectedGroup?.id

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isSelected) BrandAccent else SurfaceWhite)
                    .border(1.dp, if (isSelected) BrandAccent else BorderLight, RoundedCornerShape(24.dp))
                    .clickable { onSelectGroup(group) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else TextPrimary
                )
            }
        }

        // Direct "Add Group" chip at the end
        item {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BackgroundGray)
                    .border(1.dp, BorderLight, CircleShape)
                    .clickable { onCreateGroup() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Group", tint = TextSecondary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ModernAthleteRow(
    athlete: Individual,
    onTap: () -> Unit,
    onRemove: () -> Unit
) {
    val ageYears = remember(athlete.dateOfBirth) {
        ((System.currentTimeMillis() - athlete.dateOfBirth) / 31_557_600_000L).toInt()
    }
    val initials = remember(athlete.firstName, athlete.lastName) {
        "${athlete.firstName.first()}${athlete.lastName.first()}".uppercase()
    }
    val isRestricted = athlete.isRestricted || athlete.medicalAlert != null

    // Flat, clean list item style
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceWhite)
            .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
            .clickable { onTap() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Modern Circular Avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isRestricted) MaterialTheme.colorScheme.errorContainer else AvatarBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials,
                style = MaterialTheme.typography.titleMedium,
                color = if (isRestricted) MaterialTheme.colorScheme.onErrorContainer else BrandAccent,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Name and Stats
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${athlete.firstName} ${athlete.lastName}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Age $ageYears • ${athlete.sex.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                if (isRestricted) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.Warning, contentDescription = "Medical Alert", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                }
            }
        }

        // Action Menu / Remove
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = BrandAccent)
    }
}

@Composable
private fun ErrorState(message: String, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

@Composable
private fun EmptyGroupState(onCreateGroup: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(64.dp), tint = BorderLight)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No groups found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Text("Create a group to organise your athletes.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateGroup,
            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Create Group")
        }
    }
}

// ---------------------------------------------------------------------------
// MODERN BOTTOM SHEETS: (Replacing AlertDialogs for better UX)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGroupSheet(onDismiss: () -> Unit, onConfirm: (String, String?, String?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BorderLight) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Create New Group", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group Name *") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            
            OutlinedTextField(
                value = cycle,
                onValueChange = { cycle = it },
                label = { Text("Cycle/Term (e.g. Fall 2025)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = { onConfirm(name, location.ifBlank { null }, cycle.ifBlank { null }) },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandAccent)
            ) {
                Text("Create Group", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterAthleteSheet(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, BiologicalSex, String?, String?) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var selectedSex by remember { mutableStateOf(BiologicalSex.UNSPECIFIED) }
    var email by remember { mutableStateOf("") }
    var medicalAlert by remember { mutableStateOf("") }
    
    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BorderLight) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Register Athlete", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name *") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name *") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Date of Birth Trigger
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Cake, contentDescription = null, tint = BrandAccent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (datePickerState.selectedDateMillis != null) {
                            val date = Date(datePickerState.selectedDateMillis!!)
                            java.text.SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                        } else "Date of Birth *",
                        color = if (datePickerState.selectedDateMillis != null) TextPrimary else TextSecondary
                    )
                }
            }

            Text("Biological Sex", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BiologicalSex.entries.forEach { sex ->
                    val isSelected = selectedSex == sex
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) BrandAccent else BackgroundGray)
                            .clickable { selectedSex = sex }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            sex.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSelected) Color.White else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = medicalAlert,
                onValueChange = { medicalAlert = it },
                label = { Text("Medical Alert / Restriction") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("e.g. Asthma, Knee injury") }
            )

            Button(
                onClick = {
                    onConfirm(
                        firstName,
                        lastName,
                        datePickerState.selectedDateMillis ?: 0L,
                        selectedSex,
                        email.ifBlank { null },
                        medicalAlert.ifBlank { null }
                    )
                },
                enabled = firstName.isNotBlank() && lastName.isNotBlank() && datePickerState.selectedDateMillis != null,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandAccent)
            ) {
                Text("Register Athlete", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("OK", color = BrandAccent) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageGroupMembersSheet(
    allAthletes: List<Individual>,
    currentAthleteIds: Set<String>,
    onDismiss: () -> Unit,
    onAddAthlete: (String) -> Unit,
    onRemoveAthlete: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredAthletes = if (searchQuery.isBlank()) allAthletes else {
        allAthletes.filter { it.fullName.contains(searchQuery, ignoreCase = true) }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        dragHandle = { BottomSheetDefaults.DragHandle(color = BorderLight) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f) // Spacious but not full screen
                .padding(horizontal = 24.dp)
        ) {
            Text("Manage Group Members", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search athletes...") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredAthletes) { athlete ->
                    val isInGroup = athlete.id in currentAthleteIds
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isInGroup) AvatarBackground else Color.Transparent)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isInGroup) BrandAccent else BorderLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                athlete.firstName.first().toString().uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(athlete.fullName, fontWeight = FontWeight.SemiBold)
                            Text(athlete.sex.name, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }

                        IconButton(
                            onClick = { 
                                if (isInGroup) onRemoveAthlete(athlete.id) 
                                else onAddAthlete(athlete.id) 
                            }
                        ) {
                            Icon(
                                if (isInGroup) Icons.Default.RemoveCircleOutline else Icons.Default.AddCircleOutline,
                                contentDescription = null,
                                tint = if (isInGroup) MaterialTheme.colorScheme.error else BrandAccent
                            )
                        }
                    }
                }
            }
            
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}