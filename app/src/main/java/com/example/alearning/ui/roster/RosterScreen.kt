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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStudentReport: (String) -> Unit,
    viewModel: RosterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Roster Manager") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showAddStudentDialog() }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Student")
                    }
                    IconButton(onClick = { viewModel.showAddGroupDialog() }) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "Add Group")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Group chips
            if (uiState.groups.isNotEmpty()) {
                ScrollableTabRow(
                    selectedTabIndex = uiState.groups.indexOf(uiState.selectedGroup).coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.groups.forEachIndexed { index, group ->
                        Tab(
                            selected = group == uiState.selectedGroup,
                            onClick = { viewModel.selectGroup(group) },
                            text = { Text(group.name) }
                        )
                    }
                }
            }

            // Student list for selected group
            if (uiState.selectedGroup != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${uiState.studentsInGroup.size} students",
                        style = MaterialTheme.typography.titleSmall
                    )
                    TextButton(onClick = { viewModel.showAddToGroupDialog() }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add to Group")
                    }
                }

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.studentsInGroup) { student ->
                        ListItem(
                            headlineContent = {
                                Text("${student.firstName} ${student.lastName}")
                            },
                            supportingContent = {
                                student.medicalAlert?.let {
                                    Text(it, color = MaterialTheme.colorScheme.error)
                                }
                            },
                            leadingContent = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            trailingContent = {
                                Row {
                                    IconButton(onClick = { onNavigateToStudentReport(student.id) }) {
                                        Icon(Icons.Default.Assessment, contentDescription = "Report")
                                    }
                                    IconButton(onClick = { viewModel.removeStudentFromGroup(student.id) }) {
                                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove")
                                    }
                                }
                            },
                            modifier = Modifier.clickable { onNavigateToStudentReport(student.id) }
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

        // Add Group Dialog
        if (uiState.showAddGroupDialog) {
            AddGroupDialog(
                onDismiss = { viewModel.dismissAddGroupDialog() },
                onConfirm = { name, location, cycle ->
                    viewModel.addGroup(name, location, cycle)
                }
            )
        }

        // Add Student Dialog
        if (uiState.showAddStudentDialog) {
            AddStudentDialog(
                onDismiss = { viewModel.dismissAddStudentDialog() },
                onConfirm = { firstName, lastName, dob, sex ->
                    viewModel.addStudent(firstName, lastName, dob, sex)
                }
            )
        }

        // Add Student to Group Dialog
        if (uiState.showAddToGroupDialog) {
            AddToGroupDialog(
                allStudents = uiState.allStudents,
                currentStudentIds = uiState.studentsInGroup.map { it.id }.toSet(),
                onDismiss = { viewModel.dismissAddToGroupDialog() },
                onAddStudent = { studentId -> viewModel.addStudentToGroup(studentId) }
            )
        }
    }
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
private fun AddStudentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, Long, BiologicalSex) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var selectedSex by remember { mutableStateOf(BiologicalSex.UNSPECIFIED) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register Student") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name *") })
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name *") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BiologicalSex.entries.forEach { sex ->
                        FilterChip(
                            selected = selectedSex == sex,
                            onClick = { selectedSex = sex },
                            label = { Text(sex.name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(firstName, lastName, System.currentTimeMillis() - 410_000_000_000L, selectedSex)
                },
                enabled = firstName.isNotBlank() && lastName.isNotBlank()
            ) { Text("Register") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddToGroupDialog(
    allStudents: List<Individual>,
    currentStudentIds: Set<String>,
    onDismiss: () -> Unit,
    onAddStudent: (String) -> Unit
) {
    val availableStudents = allStudents.filter { it.id !in currentStudentIds }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Student to Group") },
        text = {
            if (availableStudents.isEmpty()) {
                Text("No available students. Register new students first.")
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(availableStudents) { student ->
                        ListItem(
                            headlineContent = { Text("${student.firstName} ${student.lastName}") },
                            trailingContent = {
                                IconButton(onClick = { onAddStudent(student.id) }) {
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
