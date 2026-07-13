package com.example.alearning.ui.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alearning.domain.model.reports.LeaderboardRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteLeaderRow(
    row: LeaderboardRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${row.rank}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color(0xFF90A4AE),
                modifier = Modifier.width(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(row.athleteName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    if (row.flagged) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFF57F17), CircleShape))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (row.rawScore != null) "${formatScore(row.rawScore)} ${row.unit}" else "Absent",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    DeltaArrow(deltaPercentile = row.deltaPercentile)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Prefer the data-driven label from the norm CSV (e.g., "Excellent", "Healthy
                // Fitness Zone") over the generic 4-zone label so coaches see the published
                // classification, not a synthetic bucket. Falls back to the generic label
                // when the snapshot has no classification (custom tests, missing norms).
                ZoneChip(
                    classification = row.classification,
                    label = row.classificationLabel?.takeIf { it.isNotBlank() }
                        ?: com.example.alearning.ui.report.components.zoneLabel(row.classification)
                )
                PercentileChip(percentile = row.percentile)
            }
        }
    }
}

private fun formatScore(s: Double): String =
    if (s % 1.0 == 0.0) s.toInt().toString() else String.format("%.1f", s)
