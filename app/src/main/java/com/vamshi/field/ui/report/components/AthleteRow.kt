package com.vamshi.field.ui.report.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.vamshi.field.domain.model.reports.LeaderboardRow
import com.vamshi.field.ui.theme.SportOrange
import com.vamshi.field.ui.theme.SportOrangeContainer

/**
 * Visual ranking hierarchy: Top 5 get the strongest emphasis, ranks 6-10 get
 * moderate emphasis, everything below is the standard row styling.
 */
private enum class LeaderRankTier { TOP_5, TOP_10, STANDARD }

private fun leaderTierFor(rank: Int): LeaderRankTier = when {
    rank <= 5 -> LeaderRankTier.TOP_5
    rank <= 10 -> LeaderRankTier.TOP_10
    else -> LeaderRankTier.STANDARD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AthleteLeaderRow(
    row: LeaderboardRow,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tier = leaderTierFor(row.rank)
    val cardElevation = when (tier) {
        LeaderRankTier.TOP_5 -> 6.dp
        LeaderRankTier.TOP_10 -> 2.dp
        LeaderRankTier.STANDARD -> 1.dp
    }
    val cardBorder = when (tier) {
        LeaderRankTier.TOP_5 -> BorderStroke(2.dp, SportOrange)
        LeaderRankTier.TOP_10 -> BorderStroke(1.dp, SportOrange.copy(alpha = 0.4f))
        LeaderRankTier.STANDARD -> null
    }
    val cardContainerColor = if (tier == LeaderRankTier.TOP_5) SportOrangeContainer else Color.White
    val rowPadding = if (tier == LeaderRankTier.TOP_5) 14.dp else 8.dp

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        border = cardBorder,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = rowPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (tier == LeaderRankTier.TOP_5) {
                Box(
                    modifier = Modifier.size(24.dp).background(SportOrange, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${row.rank}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            } else {
                Text(
                    "${row.rank}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Black,
                    color = if (tier == LeaderRankTier.TOP_10) SportOrange else Color(0xFF90A4AE),
                    modifier = Modifier.width(24.dp)
                )
            }
            Spacer(Modifier.width(if (tier == LeaderRankTier.TOP_5) 8.dp else 0.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        row.athleteName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (tier == LeaderRankTier.TOP_5) FontWeight.Bold else FontWeight.SemiBold
                    )
                    if (row.flagged) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFFF57F17), CircleShape))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (row.rawScore != null) "${formatScore(row.rawScore)} ${row.unit}" else "Absent",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    DeltaArrow(deltaPercentile = row.deltaPercentile)
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ZoneChip(
                    classification = row.classification,
                    label = row.classificationLabel?.takeIf { it.isNotBlank() }
                        ?: com.vamshi.field.ui.report.components.zoneLabel(row.classification)
                )
                if (row.percentile != null) {
                    Text(
                        "${row.percentile}%ile",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun formatScore(s: Double): String =
    if (s % 1.0 == 0.0) s.toInt().toString() else String.format("%.1f", s)
