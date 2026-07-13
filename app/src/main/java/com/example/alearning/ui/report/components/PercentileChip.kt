package com.example.alearning.ui.report.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alearning.ui.theme.PerformanceGreen
import com.example.alearning.ui.theme.PerformanceGreenText
import com.example.alearning.ui.theme.PerformanceGrey
import com.example.alearning.ui.theme.PerformanceGreyText
import com.example.alearning.ui.theme.PerformanceRed
import com.example.alearning.ui.theme.PerformanceRedText
import com.example.alearning.ui.theme.PerformanceYellow
import com.example.alearning.ui.theme.PerformanceYellowText

@Composable
fun PercentileChip(percentile: Int?, modifier: Modifier = Modifier) {
    val label = percentile?.let { "${ordinal(it)}" } ?: "—"
    val (bg, fg) = percentileChipColors(percentile)
    Text(
        text = label,
        modifier = modifier
            .background(bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        fontWeight = FontWeight.SemiBold
    )
}

// Four-zone color contract: Green ≥60, Yellow 30–59, Red <30, Grey = no data.
private fun percentileChipColors(percentile: Int?): Pair<Color, Color> = when {
    percentile == null -> PerformanceGrey to PerformanceGreyText
    percentile >= 60 -> PerformanceGreen to PerformanceGreenText
    percentile >= 30 -> PerformanceYellow to PerformanceYellowText
    else -> PerformanceRed to PerformanceRedText
}

private fun ordinal(n: Int): String {
    val suffix = when {
        n % 100 in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$n$suffix"
}
