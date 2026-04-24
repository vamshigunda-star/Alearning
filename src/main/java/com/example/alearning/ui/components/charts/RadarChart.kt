package com.example.alearning.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alearning.domain.model.standards.RadarAxis
import com.example.alearning.domain.usecase.testing.AthleteRadarData
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RadarChart(
    data: AthleteRadarData,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val axes = RadarAxis.values()
    val labels = axes.map { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
    
    // Create a map for quick lookup, default to 0 if no data for axis
    val scores = axes.map { axis ->
        data.axisScores.find { it.axis == axis }?.normalizedScore ?: 0f
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .padding(32.dp)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.width / 2
            val angleStep = (2 * Math.PI / axes.size).toFloat()

            // Draw background circles/polygons (5 levels)
            for (i in 1..5) {
                val levelRadius = radius * (i / 5f)
                val path = Path()
                for (j in axes.indices) {
                    val angle = j * angleStep - Math.PI.toFloat() / 2
                    val x = center.x + levelRadius * cos(angle)
                    val y = center.y + levelRadius * sin(angle)
                    if (j == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, Color.LightGray.copy(alpha = 0.5f), style = Stroke(width = 1.dp.toPx()))
            }

            // Draw axis lines
            for (i in axes.indices) {
                val angle = i * angleStep - Math.PI.toFloat() / 2
                val x = center.x + radius * cos(angle)
                val y = center.y + radius * sin(angle)
                drawLine(Color.LightGray.copy(alpha = 0.5f), center, Offset(x, y), strokeWidth = 1.dp.toPx())
            }

            // Draw data polygon
            val dataPath = Path()
            for (i in scores.indices) {
                val score = scores[i].coerceIn(0f, 1f)
                val angle = i * angleStep - Math.PI.toFloat() / 2
                val x = center.x + radius * score * cos(angle)
                val y = center.y + radius * score * sin(angle)
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()
            drawPath(dataPath, color.copy(alpha = 0.3f))
            drawPath(dataPath, color, style = Stroke(width = 2.dp.toPx()))
            
            // Draw points
            for (i in scores.indices) {
                val score = scores[i].coerceIn(0f, 1f)
                val angle = i * angleStep - Math.PI.toFloat() / 2
                val x = center.x + radius * score * cos(angle)
                val y = center.y + radius * score * sin(angle)
                drawCircle(color, radius = 4.dp.toPx(), center = Offset(x, y))
            }
        }

        // Labels
        axes.forEachIndexed { i, _ ->
            val angle = i * (2 * Math.PI / axes.size).toFloat() - Math.PI.toFloat() / 2
            val labelRadius = 140.dp // Slightly more than canvas size / 2
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(
                        x = (labelRadius.value * cos(angle)).dp,
                        y = (labelRadius.value * sin(angle)).dp
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labels[i],
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
