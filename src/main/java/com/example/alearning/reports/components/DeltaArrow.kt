package com.example.alearning.reports.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.alearning.ui.theme.PerformanceGreenText
import com.example.alearning.ui.theme.PerformanceRedText

@Composable
fun DeltaArrow(deltaPercentile: Int?, modifier: Modifier = Modifier) {
    if (deltaPercentile == null) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Icon(
                Icons.AutoMirrored.Filled.TrendingFlat,
                contentDescription = null,
                tint = Color(0xFF9E9E9E),
                modifier = Modifier.size(14.dp)
            )
            Text("—", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9E9E9E))
        }
        return
    }
    val (icon, color) = when {
        deltaPercentile > 0 -> Icons.AutoMirrored.Filled.TrendingUp to PerformanceGreenText
        deltaPercentile < 0 -> Icons.AutoMirrored.Filled.TrendingDown to PerformanceRedText
        else -> Icons.AutoMirrored.Filled.TrendingFlat to Color(0xFF9E9E9E)
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Text(
            text = if (deltaPercentile == 0) "0" else (if (deltaPercentile > 0) "+$deltaPercentile" else "$deltaPercentile"),
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}
