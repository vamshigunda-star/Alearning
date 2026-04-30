package com.example.alearning.reports.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.alearning.interpretation.Distribution

@Composable
fun DistributionBar(
    distribution: Distribution,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp
) {
    val total = distribution.total.coerceAtLeast(1)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFECEFF1))
    ) {
        if (distribution.superior > 0) {
            Box(
                modifier = Modifier
                    .weight(distribution.superior.toFloat() / total)
                    .background(Color(0xFF1B5E20))
            )
        }
        if (distribution.healthy > 0) {
            Box(
                modifier = Modifier
                    .weight(distribution.healthy.toFloat() / total)
                    .background(Color(0xFF0D47A1))
            )
        }
        if (distribution.needsImprovement > 0) {
            Box(
                modifier = Modifier
                    .weight(distribution.needsImprovement.toFloat() / total)
                    .background(Color(0xFFB71C1C))
            )
        }
        if (distribution.noData > 0) {
            Box(
                modifier = Modifier
                    .weight(distribution.noData.toFloat() / total)
                    .background(Color(0xFFCFD8DC))
            )
        }
    }
}
