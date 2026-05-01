package com.example.alearning.ui.athlete

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.ui.components.AppTopBar
import com.example.alearning.ui.components.AppTopBarSubtitleColor
import com.example.alearning.reports.AttemptRow
import com.example.alearning.reports.LeaderboardRow
import com.example.alearning.reports.components.ChartPoint
import com.example.alearning.reports.components.DeltaArrow
import com.example.alearning.reports.components.NormBandLineChart
import com.example.alearning.reports.components.PercentileChip
import com.example.alearning.reports.components.ZoneChip
import com.example.alearning.reports.components.zoneColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton

@Composable
fun AthleteTestDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: AthleteTestDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    AthleteTestDetailContent(
        uiState = uiState,
        onAction = { action ->
            when (action) {
                AthleteTestDetailAction.OnNavigateBack -> onNavigateBack()
                else -> viewModel.onAction(action)
            }
        }
    )
    
    uiState.deleteCandidate?.let { attempt ->
        val df = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { viewModel.onAction(AthleteTestDetailAction.OnDismissDelete) },
            title = { Text("Delete Test Result?") },
            text = { 
                val scoreStr = if (attempt.rawScore % 1.0 == 0.0) attempt.rawScore.toInt().toString() else String.format("%.1f", attempt.rawScore)
                Text("This will permanently remove the score of $scoreStr recorded on ${df.format(Date(attempt.date))}. This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onAction(AthleteTestDetailAction.OnConfirmDelete) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onAction(AthleteTestDetailAction.OnDismissDelete) }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteTestDetailContent(
    uiState: AthleteTestDetailUiState,
    onAction: (AthleteTestDetailAction) -> Unit
) {
    val data = uiState.data
    Scaffold(
        topBar = {
            AppTopBar(
                title = {
                    Column {
                        Text(data?.test?.name ?: "Test", style = MaterialTheme.typography.titleLarge)
                        data?.athlete?.fullName?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = AppTopBarSubtitleColor)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(AthleteTestDetailAction.OnNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            data == null -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No data", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> DetailBody(uiState = uiState, padding = padding, onAction = onAction)
        }

        if (uiState.showPeerSheet && data != null) {
            ModalBottomSheet(onDismissRequest = { onAction(AthleteTestDetailAction.OnDismissPeerSheet) }) {
                PeerSheet(
                    rows = data.peerLeaderboard.orEmpty(),
                    highlightId = data.athlete.id,
                    title = "${data.test.name} · peers"
                )
            }
        }
    }
}

@Composable
private fun DetailBody(
    uiState: AthleteTestDetailUiState,
    padding: PaddingValues,
    onAction: (AthleteTestDetailAction) -> Unit
) {
    val data = uiState.data!!
    val latest = data.attempts.lastOrNull()
    val df = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val cls = latest?.classification ?: com.example.alearning.interpretation.Classification.NO_DATA
            val colors = zoneColors(cls)
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = colors.bg)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (latest != null) "Latest result · ${df.format(Date(latest.date))}" else "Latest result",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.fg
                    )
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = latest?.let { formatScore(it.rawScore) } ?: "—",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = colors.fg
                        )
                        Text(data.test.unit, style = MaterialTheme.typography.titleMedium, color = colors.fg, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        ZoneChip(classification = cls)
                        PercentileChip(percentile = latest?.percentile)
                        latest?.classificationLabel?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = colors.fg)
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    if (data.attempts.size < 2) {
                        Box(modifier = Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            Text("Need 2+ attempts to chart", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        NormBandLineChart(
                            points = data.attempts.map { ChartPoint(it.date, it.rawScore) },
                            bands = data.bandsByDate,
                            isHigherBetter = data.test.isHigherBetter,
                            unit = data.test.unit,
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LegendRow()
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Attempts (${data.attempts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (data.peerLeaderboard != null) {
                    OutlinedButton(onClick = { onAction(AthleteTestDetailAction.OnOpenPeerSheet) }) {
                        Icon(Icons.Default.Groups, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Compare to peers")
                    }
                }
            }
        }

        if (data.attempts.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No attempts yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(data.attempts.reversed()) { row -> 
                AttemptRowView(
                    row = row, 
                    unit = data.test.unit,
                    onDelete = { onAction(AthleteTestDetailAction.OnRequestDelete(row)) }
                ) 
            }
        }
    }
}

@Composable
private fun AttemptRowView(row: AttemptRow, unit: String, onDelete: () -> Unit) {
    val df = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(df.format(Date(row.date)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${formatScore(row.rawScore)} $unit", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                row.classificationLabel?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ZoneChip(classification = row.classification)
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    DeltaArrow(deltaPercentile = row.deltaPercentile)
                    PercentileChip(percentile = row.percentile)
                }
            }
        }
    }
}

@Composable
private fun PeerSheet(rows: List<LeaderboardRow>, highlightId: String, title: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (rows.isEmpty()) {
            Text("No peer results for this session.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        }
        rows.forEach { r ->
            val highlight = r.individualId == highlightId
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (highlight) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, RoundedCornerShape(6.dp))
                    .padding(vertical = 8.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${r.rank}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(28.dp))
                Text(r.athleteName, modifier = Modifier.weight(1f), fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal)
                Text(r.rawScore?.let { formatScore(it) } ?: "—", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                ZoneChip(classification = r.classification)
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LegendRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendDot(Color(0xFF1B5E20), "Superior")
        LegendDot(Color(0xFF0D47A1), "Healthy")
        LegendDot(Color(0xFFB71C1C), "Needs Imp.")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.width(10.dp).height(10.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatScore(s: Double): String =
    if (s % 1.0 == 0.0) s.toInt().toString() else String.format("%.1f", s)
