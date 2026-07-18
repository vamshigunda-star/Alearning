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
import com.example.alearning.domain.model.reports.Classification
import com.example.alearning.domain.usecase.reports.ClassifyPercentileUseCase
import com.example.alearning.ui.theme.PerformanceGreen
import com.example.alearning.ui.theme.PerformanceGreenText
import com.example.alearning.ui.theme.PerformanceGrey
import com.example.alearning.ui.theme.PerformanceGreyText
import com.example.alearning.ui.theme.PerformanceRed
import com.example.alearning.ui.theme.PerformanceRedText
import com.example.alearning.ui.theme.PerformanceYellow
import com.example.alearning.ui.theme.PerformanceYellowText

private val classifyPercentile = ClassifyPercentileUseCase()

@Composable
fun PercentileChip(percentile: Int?, modifier: Modifier = Modifier) {
    val label = percentile?.let { "${ordinal(it)}" } ?: "—"
    val (bg, fg) = percentileChipColors(classifyPercentile(percentile))
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

private fun percentileChipColors(classification: Classification): Pair<Color, Color> = when (classification) {
    Classification.NO_DATA -> PerformanceGrey to PerformanceGreyText
    Classification.SUPERIOR -> PerformanceGreen to PerformanceGreenText
    Classification.HEALTHY -> PerformanceYellow to PerformanceYellowText
    Classification.NEEDS_IMPROVEMENT -> PerformanceRed to PerformanceRedText
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
