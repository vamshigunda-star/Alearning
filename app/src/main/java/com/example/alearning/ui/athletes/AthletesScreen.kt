package com.example.alearning.ui.athletes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.people.BiologicalSex
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.ui.components.AppTopBar
import com.example.alearning.ui.components.RegisterAthleteSheet
import com.example.alearning.ui.theme.SportOrange
import com.example.alearning.ui.theme.SportOrangeContainer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthletesScreen(
    onNavigateToAthleteReport: (String) -> Unit,
    viewModel: AthletesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = "Athletes",
                actions = {
                    IconButton(onClick = { viewModel.onAction(AthletesAction.OnShowRegisterSheet) }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Register Athlete")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SportOrange)
                    }
                }
                uiState.allAthletes.isEmpty() -> {
                    EmptyAthletesState(onRegister = { viewModel.onAction(AthletesAction.OnShowRegisterSheet) })
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Search & Filter
                        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                OutlinedTextField(
                                    value = uiState.searchQuery,
                                    onValueChange = { viewModel.onAction(AthletesAction.OnSearchQueryChange(it)) },
                                    placeholder = { Text("Search athletes...") },
                                    modifier = Modifier.fillMaxWidth(),
                                    leadingIcon = { Icon(Icons.Default.Search, null) },
                                    shape = RoundedCornerShape(12.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = uiState.sexFilter == null,
                                        onClick = { viewModel.onAction(AthletesAction.OnSexFilterChange(null)) },
                                        label = { Text("All") }
                                    )
                                    FilterChip(
                                        selected = uiState.sexFilter == BiologicalSex.MALE,
                                        onClick = { viewModel.onAction(AthletesAction.OnSexFilterChange(BiologicalSex.MALE)) },
                                        label = { Text("Male") }
                                    )
                                    FilterChip(
                                        selected = uiState.sexFilter == BiologicalSex.FEMALE,
                                        onClick = { viewModel.onAction(AthletesAction.OnSexFilterChange(BiologicalSex.FEMALE)) },
                                        label = { Text("Female") }
                                    )
                                    FilterChip(
                                        selected = uiState.sexFilter == BiologicalSex.UNSPECIFIED,
                                        onClick = { viewModel.onAction(AthletesAction.OnSexFilterChange(BiologicalSex.UNSPECIFIED)) },
                                        label = { Text("Unspecified") }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.filteredAthletes.size} of ${uiState.allAthletes.size} Athletes",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(uiState.filteredAthletes, key = { it.id }) { athlete ->
                                AthleteDirectoryRow(
                                    athlete = athlete,
                                    onTap = { onNavigateToAthleteReport(athlete.id) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }

    if (uiState.showRegisterSheet) {
        RegisterAthleteSheet(
            onDismiss = { viewModel.onAction(AthletesAction.OnDismissRegisterSheet) },
            onConfirm = { first, last, dob, sex, email, med -> 
                viewModel.onAction(AthletesAction.OnRegisterAthlete(first, last, dob, sex, email, med)) 
            }
        )
    }
    
    if (uiState.errorMessage != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = { TextButton(onClick = { viewModel.onAction(AthletesAction.OnDismissError) }) { Text("Dismiss") } }
        ) {
            Text(uiState.errorMessage!!)
        }
    }
}

@Composable
private fun AthleteDirectoryRow(
    athlete: Individual,
    onTap: () -> Unit
) {
    val ageYears = remember(athlete.dateOfBirth) {
        ((System.currentTimeMillis() - athlete.dateOfBirth) / 31_557_600_000L).toInt()
    }
    val initials = remember(athlete.firstName, athlete.lastName) {
        "${athlete.firstName.first()}${athlete.lastName.first()}".uppercase()
    }
    val isRestricted = athlete.isRestricted || athlete.medicalAlert != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .clickable { onTap() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isRestricted) MaterialTheme.colorScheme.errorContainer else SportOrangeContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials,
                style = MaterialTheme.typography.titleMedium,
                color = if (isRestricted) MaterialTheme.colorScheme.onErrorContainer else SportOrange,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${athlete.firstName} ${athlete.lastName}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Age $ageYears • ${athlete.sex.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isRestricted) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.Warning, contentDescription = "Medical Alert", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                }
            }
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyAthletesState(onRegister: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.PersonSearch, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No athletes found", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text("Register an athlete to get started.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRegister,
            colors = ButtonDefaults.buttonColors(containerColor = SportOrange),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("Register Athlete")
        }
    }
}
