package com.example.alearning.ui.roster

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.model.people.Individual
import java.text.SimpleDateFormat
import java.util.*

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
        topBar = {
            TopAppBar(
                title = { Text("Roster Manager") },
                navigationIcon = {
                    IconButton(onClick = { onAction(RosterAction.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onAction(RosterAction.OnShowRegisterAthleteDialog) }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Register Athlete")
                    }
                    IconButton(onClick = { onAction(RosterAction.OnShowAddGroupDialog) }) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "Create Group")
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
                        TextButton(onClick = { onAction(RosterAction.OnDismissError) }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
            else -> {
                RosterBody(uiState = uiState, onAction = onAction, padding = padding)
            }
        }
    }

    if (uiState.showAddGroupDialog) {
        AddGroupDialog(
            onDismiss = { onAction(RosterAction.OnDismissAddGroupDialog) },
            onConfirm = { name, location, cycle ->
                onAction(RosterAction.OnCreateGroup(name, location, cycle))
            }
        )
    }

    if (uiState.showRegisterAthleteDialog) {
        RegisterAthleteDialog(
            onDismiss = { onAction(RosterAction.OnDismissRegisterAthleteDialog) },
            onConfirm = { firstName, lastName, dob, sex, email, medicalAlert ->
                onAction(RosterAction.OnRegisterAthlete(firstName, lastName, dob, sex, email, medicalAlert))
            }
        )
    }

    if (uiState.showAddToGroupDialog) {
        AddToGroupDialog(
            allAthletes = uiState.allAthletes,
            currentAthleteIds = uiState.athletesInGroup.map { it.id }.toSet(),
            onDismiss = { onAction(RosterAction.OnDismissAddToGroupDialog) },
            onAddAthlete = { id -> onAction(RosterAction.OnAddAthleteToGroup(id)) }
        )
    }
}

@Composable
private fun RosterBody(
    uiState: RosterUiState,
    onAction: (RosterAction) -> Unit,
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        if (uiState.groups.isNotEmpty()) {
            ScrollableTabRow(
                selectedTabIndex = uiState.groups.indexOf(uiState.selectedGroup).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth()
            ) {
                uiState.groups.forEachIndexed { _, group ->
                    Tab(
                        selected = group == uiState.selectedGroup,
                        onClick = { onAction(RosterAction.OnSelectGroup(group)) },
                        text = { Text(group.name) }
                    )
                }
            }
        }

        if (uiState.selectedGroup != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${uiState.athletesInGroup.size} athletes",
                    style = MaterialTheme.typography.titleSmall
                )
                TextButton(onClick = { onAction(RosterAction.OnShowAddToGroupDialog) }) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add to Group")
                }
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(uiState.athletesInGroup) { athlete ->
                    AthleteListItem(
                        athlete = athlete,
                        onTap = { onAction(RosterAction.OnNavigateToAthleteReport(athlete.id)) },
                        onRemove = { onAction(RosterAction.OnRemoveAthleteFromGroup(athlete.id)) }
                    )
                    HorizontalDivider()
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Create a group to get started")
            }
        }
    }
}

@Composable
private fun AthleteListItem(
    athlete: Individual,
    onTap: () -> Unit,
    onRemove: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text("${athlete.firstName} ${athlete.lastName}")
        },
        supportingContent = {
            Column {
                if (athlete.medicalAlert != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Medical alert",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            athlete.medicalAlert,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (athlete.email != null) {
                    Text(
                        athlete.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        leadingContent = {
            Icon(
                if (athlete.isRestricted) Icons.Default.Warning else Icons.Default.Person,
                contentDescription = null,
                tint = if (athlete.isRestricted) MaterialTheme.colorScheme.error
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove from group")
            }
        },
        modifier = Modifier.clickable(onClick = onTap)
    )
}

@Composable
private fun AddGroupDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var cycle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Group Name *") })
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") })
                OutlinedTextField(value = cycle, onValueChange = { cycle = it }, label = { Text("Cycle/Term") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, location.ifBlank { null }, cycle.ifBlank { null }) },
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RegisterAthleteDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, BiologicalSex, String?, String?) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var selectedSex by remember { mutableStateOf(BiologicalSex.UNSPECIFIED) }
    var dobDay by remember { mutableStateOf("") }
    var dobMonth by remember { mutableStateOf("") }
    var dobYear by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var medicalAlert by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Athlete") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name *") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Date of Birth *", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dobDay,
                        onValueChange = { if (it.length <= 2) dobDay = it.filter { c -> c.isDigit() } },
                        label = { Text("DD") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dobMonth,
                        onValueChange = { if (it.length <= 2) dobMonth = it.filter { c -> c.isDigit() } },
                        label = { Text("MM") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = dobYear,
                        onValueChange = { if (it.length <= 4) dobYear = it.filter { c -> c.isDigit() } },
                        label = { Text("YYYY") },
                        modifier = Modifier.weight(1.5f)
                    )
                }
                Text("Biological Sex", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BiologicalSex.entries.forEach { sex ->
                        FilterChip(
                            selected = selectedSex == sex,
                            onClick = { selectedSex = sex },
                            label = { Text(sex.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = medicalAlert,
                    onValueChange = { medicalAlert = it },
                    label = { Text("Medical Alert") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val dobValid = dobDay.toIntOrNull() in 1..31 &&
                    dobMonth.toIntOrNull() in 1..12 &&
                    dobYear.toIntOrNull() in 1900..2025
            TextButton(
                onClick = {
                    val cal = Calendar.getInstance().apply {
                        set(dobYear.toInt(), dobMonth.toInt() - 1, dobDay.toInt(), 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onConfirm(
                        firstName, lastName, cal.timeInMillis, selectedSex,
                        email.ifBlank { null }, medicalAlert.ifBlank { null }
                    )
                },
                enabled = firstName.isNotBlank() && lastName.isNotBlank() && dobValid
            ) { Text("Register") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddToGroupDialog(
    allAthletes: List<Individual>,
    currentAthleteIds: Set<String>,
    onDismiss: () -> Unit,
    onAddAthlete: (String) -> Unit
) {
    val availableAthletes = allAthletes.filter { it.id !in currentAthleteIds }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Athlete to Group") },
        text = {
            if (availableAthletes.isEmpty()) {
                Text("No available athletes. Register new athletes first.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(availableAthletes) { athlete ->
                        ListItem(
                            headlineContent = { Text("${athlete.firstName} ${athlete.lastName}") },
                            trailingContent = {
                                IconButton(onClick = { onAddAthlete(athlete.id) }) {
                                    Icon(Icons.Default.Add, contentDescription = "Add")
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
