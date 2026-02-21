package com.example.alearning.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.domain.model.people.Individual
import com.example.alearning.domain.usecase.testing.AthleteTestSummary
import com.example.alearning.domain.usecase.testing.IndividualProgress
import com.example.alearning.domain.usecase.testing.ProgressDataPoint
import com.example.alearning.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AthleteReportScreen(
    onNavigateBack: () -> Unit,
    viewModel: AthleteReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    AthleteReportContent(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                is AthleteReportAction.OnNavigateBack -> onNavigateBack()
                else -> viewModel.onAction(action)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteReportContent(
    uiState: AthleteReportUiState,
    onAction: (AthleteReportAction) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Athlete Report") },
                navigationIcon = {
                    IconButton(onClick = { onAction(AthleteReportAction.OnNavigateBack) }) {
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
            uiState.errorMessage != null && uiState.profile == null -> {
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
                        TextButton(onClick = { onAction(AthleteReportAction.OnDismissError) }) {
                            Text("Dismiss")
                        }
                    }
                }
            }
            else -> {
                AthleteReportBody(uiState = uiState, onAction = onAction, padding = padding)
            }
        }
    }
}

@Composable
private fun AthleteReportBody(
    uiState: AthleteReportUiState,
    onAction: (AthleteReportAction) -> Unit,
    padding: PaddingValues
) {
    val profile = uiState.profile
    val summaries = profile?.summaries ?: emptyList()
    val groupedSummaries = summaries.groupBy { it.categoryName }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Athlete info header
        uiState.athlete?.let { athlete ->
            item {
                AthleteHeaderCard(athlete)
            }
        }

        if (summaries.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No test results yet")
                    }
                }
            }
        } else {
            // Group results by category
            groupedSummaries.forEach { (categoryName, categorySummaries) ->
                item {
                    Text(
                        categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(categorySummaries) { summary ->
                    TestSummaryCard(
                        summary = summary,
                        isSelected = summary.testId == uiState.selectedTestId,
                        onClick = { onAction(AthleteReportAction.OnSelectTest(summary.testId)) }
                    )
                }
            }

            // Progress section (lazy loaded on test tap)
            if (uiState.isProgressLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                }
            }

            uiState.selectedTestProgress?.let { progress ->
                item {
                    ProgressSection(progress = progress)
                }
            }
        }
    }
}

@Composable
private fun AthleteHeaderCard(athlete: Individual) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Column {
                Text(
                    "${athlete.firstName} ${athlete.lastName}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Sex: ${athlete.sex.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium
                )
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
            }
        }
    }
}

@Composable
private fun TestSummaryCard(
    summary: AthleteTestSummary,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val (bgColor, textColor, borderColor) = when {
        summary.percentile == null -> Triple(PerformanceGrey, PerformanceGreyText, PerformanceGreyBorder)
        summary.percentile >= 60 -> Triple(PerformanceGreen, PerformanceGreenText, PerformanceGreenBorder)
        summary.percentile >= 30 -> Triple(PerformanceYellow, PerformanceYellowText, PerformanceYellowBorder)
        else -> Triple(PerformanceRed, PerformanceRedText, PerformanceRedBorder)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    summary.testName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${summary.latestScore} ${summary.unit}",
                    style = MaterialTheme.typography.bodyMedium
                )
                summary.classification?.let { cls ->
                    Text(
                        cls,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${summary.totalAttempts} attempt${if (summary.totalAttempts != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            summary.percentile?.let { p ->
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(bgColor, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${p}%",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgressSection(progress: IndividualProgress) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${progress.testName} — Progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (progress.dataPoints.isEmpty()) {
                Text("No data points", style = MaterialTheme.typography.bodyMedium)
            } else {
                progress.dataPoints.forEach { point ->
                    ProgressPointRow(point = point, isHigherBetter = progress.isHigherBetter)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun ProgressPointRow(point: ProgressDataPoint, isHigherBetter: Boolean) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                dateFormat.format(Date(point.date)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${point.rawScore} ${point.unit}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            point.delta?.let { delta ->
                val improved = point.isImproved == true
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (improved) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (improved) PerformanceGreenText else PerformanceRedText,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        String.format("%+.1f", delta),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (improved) PerformanceGreenText else PerformanceRedText
                    )
                }
            }

            point.percentile?.let { p ->
                val (bgColor, textColor) = when {
                    p >= 60 -> PerformanceGreen to PerformanceGreenText
                    p >= 30 -> PerformanceYellow to PerformanceYellowText
                    else -> PerformanceRed to PerformanceRedText
                }
                Box(
                    modifier = Modifier
                        .background(bgColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("${p}%", style = MaterialTheme.typography.labelSmall, color = textColor)
                }
            }
        }
    }
}
