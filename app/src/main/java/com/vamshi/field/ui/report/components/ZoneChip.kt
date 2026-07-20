package com.vamshi.field.ui.report.components

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
import com.vamshi.field.domain.model.reports.Classification
import com.vamshi.field.ui.theme.PerformanceGreen
import com.vamshi.field.ui.theme.PerformanceGreenText
import com.vamshi.field.ui.theme.PerformanceGrey
import com.vamshi.field.ui.theme.PerformanceGreyText
import com.vamshi.field.ui.theme.PerformanceRed
import com.vamshi.field.ui.theme.PerformanceRedText
import com.vamshi.field.ui.theme.PerformanceYellow
import com.vamshi.field.ui.theme.PerformanceYellowText

data class ZoneColors(val bg: Color, val fg: Color)

// Maps engine Classification onto the four-zone color contract documented in CLAUDE.md.
// HEALTHY is the mid (Yellow 30–59) zone — never blue.
fun zoneColors(c: Classification): ZoneColors = when (c) {
    Classification.SUPERIOR -> ZoneColors(PerformanceGreen, PerformanceGreenText)
    Classification.HEALTHY -> ZoneColors(PerformanceYellow, PerformanceYellowText)
    Classification.NEEDS_IMPROVEMENT -> ZoneColors(PerformanceRed, PerformanceRedText)
    Classification.NO_DATA -> ZoneColors(PerformanceGrey, PerformanceGreyText)
}

fun zoneLabel(c: Classification): String = when (c) {
    Classification.SUPERIOR -> "Superior"
    Classification.HEALTHY -> "Healthy"
    Classification.NEEDS_IMPROVEMENT -> "Needs Imp."
    Classification.NO_DATA -> "—"
}

@Composable
fun ZoneChip(
    classification: Classification,
    modifier: Modifier = Modifier,
    label: String = zoneLabel(classification)
) {
    val colors = zoneColors(classification)
    Text(
        text = label,
        modifier = modifier
            .background(colors.bg, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = colors.fg,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun PerformanceYellowChip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .background(PerformanceYellow, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelSmall,
        color = PerformanceYellowText,
        fontWeight = FontWeight.SemiBold
    )
}
