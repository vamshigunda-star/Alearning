package com.example.alearning.ui.components.charts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.alearning.domain.model.reports.LeaderboardRow
import com.example.alearning.ui.report.components.zoneColors

/**
 * A clean, high-density horizontal bar chart for mobile rosters.
 *
 * It represents athlete performance using percentile lengths (0-100)
 * rather than raw scores, ensuring that bars scale consistently across
 * different tests (cm, sec, kg) and that longer bars always represent
 * superior performance.
 */
@Composable
fun HorizontalBarChart(
    rows: List<LeaderboardRow>,
    modifier: Modifier = Modifier
) {
    val validRows = rows.filter { it.rawScore != null && !it.absent }
    if (validRows.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("No scores available to chart", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(rows) {
        startAnimation = false
        kotlinx.coroutines.delay(50)
        startAnimation = true
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        validRows.forEach { row ->
            val percentile = row.percentile ?: 0
            val targetFraction = (percentile.toFloat() / 100f).coerceIn(0.03f, 1f)
            
            val animatedFraction by animateFloatAsState(
                targetValue = if (startAnimation) targetFraction else 0f,
                animationSpec = tween(durationMillis = 600),
                label = "barWidth"
            )

            val colors = zoneColors(row.classification)
            val scoreStr = if (row.rawScore != null) {
                val s = row.rawScore
                if (s % 1.0 == 0.0) s.toInt().toString() else String.format("%.1f", s)
            } else "—"

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = row.athleteName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(110.dp)
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedFraction)
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.bg)
                    )
                }

                Text(
                    text = "$scoreStr ${row.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(70.dp)
                )
            }
        }
    }
}
