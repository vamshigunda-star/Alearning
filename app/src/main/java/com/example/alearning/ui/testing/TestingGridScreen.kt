package com.example.alearning.ui.testing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.model.standards.FitnessTest
import com.example.alearning.domain.model.testing.TestResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestingGridScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStudentReport: (String) -> Unit,
    viewModel: TestingGridViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Testing Grid") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val gridData = uiState.gridData
            if (gridData == null || gridData.tests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No tests configured for this event")
                }
            } else {
                val horizontalScroll = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        // Student name column header
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .padding(8.dp)
                        ) {
                            Text(
                                "Student",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Test column headers
                        Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
                            gridData.tests.forEach { test ->
                                Box(
                                    modifier = Modifier
                                        .width(90.dp)
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        test.name.take(12),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Student rows
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(gridData.students) { student ->
                            StudentRow(
                                student = student,
                                tests = gridData.tests,
                                results = gridData.results,
                                horizontalScroll = horizontalScroll,
                                onCellClick = { test -> viewModel.startEditing(student, test) },
                                onStudentClick = { onNavigateToStudentReport(student.id) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        // Score entry dialog
        uiState.editingCell?.let { cell ->
            ScoreEntryDialog(
                studentName = "${cell.student.firstName} ${cell.student.lastName}",
                testName = cell.test.name,
                unit = cell.test.unit,
                currentScore = cell.currentResult?.rawScore,
                onDismiss = { viewModel.dismissEditing() },
                onSave = { score -> viewModel.saveScore(score) }
            )
        }
    }
}

@Composable
private fun StudentRow(
    student: Individual,
    tests: List<FitnessTest>,
    results: List<TestResult>,
    horizontalScroll: androidx.compose.foundation.ScrollState,
    onCellClick: (FitnessTest) -> Unit,
    onStudentClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Student name
        Box(
            modifier = Modifier
                .width(140.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onStudentClick
                )
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                "${student.lastName}, ${student.firstName.first()}.",
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Score cells
        Row(modifier = Modifier.horizontalScroll(horizontalScroll)) {
            tests.forEach { test ->
                val result = results.find {
                    it.individualId == student.id && it.testId == test.id
                }
                ScoreCell(
                    result = result,
                    onClick = { onCellClick(test) }
                )
            }
        }
    }
}

@Composable
private fun ScoreCell(
    result: TestResult?,
    onClick: () -> Unit
) {
    val bgColor = when {
        result == null -> Color.Transparent
        result.percentile == null -> MaterialTheme.colorScheme.surfaceVariant
        result.percentile >= 60 -> Color(0xFFC8E6C9) // Green
        result.percentile >= 30 -> Color(0xFFFFF9C4) // Yellow
        else -> Color(0xFFFFCDD2) // Red
    }

    Box(
        modifier = Modifier
            .width(90.dp)
            .height(44.dp)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            .background(bgColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (result != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    String.format("%.1f", result.rawScore),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                result.percentile?.let { p ->
                    Text(
                        "${p}%",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Text("--", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable
private fun ScoreEntryDialog(
    studentName: String,
    testName: String,
    unit: String,
    currentScore: Double?,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var scoreText by remember { mutableStateOf(currentScore?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enter Score") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(studentName, fontWeight = FontWeight.Bold)
                Text(testName, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { scoreText = it },
                    label = { Text("Score ($unit)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scoreText.toDoubleOrNull()?.let { onSave(it) }
                },
                enabled = scoreText.toDoubleOrNull() != null
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
