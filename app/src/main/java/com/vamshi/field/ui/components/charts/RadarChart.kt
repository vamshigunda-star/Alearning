package com.vamshi.field.ui.components.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Brush
import com.vamshi.field.domain.usecase.testing.AthleteRadarData
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Spider/radar chart driven by `data.axisScores`.
 *
 * The list itself is the source of truth for the spoke set and labels — we no
 * longer iterate `RadarAxis.values()`, because the seeded categories don't
 * cover every enum value (which used to leave permanent zero spokes for
 * SPEED / BALANCE on the Athlete dashboard).
 *
 * The chart and labels are drawn on a single Canvas so positioning is exact
 * and labels can never end up clipped by an outer Box of the wrong size.
 */
@Composable
fun RadarChart(
    data: AthleteRadarData,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val scores = data.axisScores
    if (scores.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSurface
    val labelStyle = remember(labelColor) {
        TextStyle(color = labelColor, fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, textAlign = TextAlign.Center)
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            val n = scores.size
            // Reserve space around the polygon for labels (~32dp ring).
            val labelInset = 32.dp.toPx()
            val maxRadius = (minOf(size.width, size.height) / 2f) - labelInset
            if (maxRadius <= 0f) return@Canvas

            val center = Offset(size.width / 2f, size.height / 2f)
            val angleStep = (2 * Math.PI / n).toFloat()
            // Start at top, go clockwise.
            fun angleAt(i: Int): Float = i * angleStep - (Math.PI.toFloat() / 2f)

            // 5 background rings.
            val gridColor = Color.LightGray.copy(alpha = 0.4f)
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            for (level in 1..5) {
                val r = maxRadius * (level / 5f)
                val ringPath = Path()
                for (i in 0 until n) {
                    val a = angleAt(i)
                    val x = center.x + r * cos(a)
                    val y = center.y + r * sin(a)
                    if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                }
                ringPath.close()
                drawPath(ringPath, gridColor, style = Stroke(width = 1.dp.toPx(), pathEffect = dashEffect))
            }

            // Spokes from center to each axis tip.
            for (i in 0 until n) {
                val a = angleAt(i)
                val x = center.x + maxRadius * cos(a)
                val y = center.y + maxRadius * sin(a)
                drawLine(gridColor, center, Offset(x, y), strokeWidth = 1.dp.toPx(), pathEffect = dashEffect)
            }

            // Data polygon.
            val dataPath = Path()
            var anyData = false
            for (i in 0 until n) {
                // Ensure a minimum size (e.g. 0.05f) so it forms a small polygon instead of a point
                val s = maxOf(scores[i].normalizedScore.coerceIn(0f, 1f), 0.05f)
                if (scores[i].normalizedScore > 0f) anyData = true
                val a = angleAt(i)
                val x = center.x + maxRadius * s * cos(a)
                val y = center.y + maxRadius * s * sin(a)
                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()

            val gradientBrush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.6f), color.copy(alpha = 0.1f)),
                center = center,
                radius = maxRadius
            )
            drawPath(dataPath, gradientBrush)
            drawPath(dataPath, color, style = Stroke(width = 2.dp.toPx()))

            // Vertices.
            for (i in 0 until n) {
                val s = maxOf(scores[i].normalizedScore.coerceIn(0f, 1f), 0.05f)
                val a = angleAt(i)
                val x = center.x + maxRadius * s * cos(a)
                val y = center.y + maxRadius * s * sin(a)
                val pt = Offset(x, y)
                // Glow effect
                drawCircle(color.copy(alpha = 0.3f), radius = 8.dp.toPx(), center = pt)
                // Point center
                drawCircle(Color.White, radius = 4.dp.toPx(), center = pt)
                // Border
                drawCircle(color, radius = 4.dp.toPx(), center = pt, style = Stroke(width = 1.5.dp.toPx()))
            }

            // Labels — drawn on the same canvas so positioning is exact.
            // We fit each label inside a max width and centre it on the spoke tip,
            // then nudge inward/outward by half the text size so it doesn't overlap
            // the polygon vertex.
            val labelMaxWidth = (size.width / n.coerceAtLeast(3)).coerceAtMost(120.dp.toPx())
            for (i in 0 until n) {
                val a = angleAt(i)
                val tipX = center.x + (maxRadius + 4.dp.toPx()) * cos(a)
                val tipY = center.y + (maxRadius + 4.dp.toPx()) * sin(a)
                val measured = textMeasurer.measure(
                    text = scores[i].label,
                    style = labelStyle,
                    maxLines = 2,
                    constraints = androidx.compose.ui.unit.Constraints(
                        maxWidth = labelMaxWidth.toInt()
                    )
                )
                val tw = measured.size.width
                val th = measured.size.height
                // Anchor the text so it sits just outside the spoke tip.
                val anchorX = tipX + cos(a) * (tw / 2f) - tw / 2f
                val anchorY = tipY + sin(a) * (th / 2f) - th / 2f
                // Clamp to canvas bounds so nothing gets cropped on small screens.
                val safeX = anchorX.coerceIn(0f, size.width - tw)
                val safeY = anchorY.coerceIn(0f, size.height - th)
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(safeX, safeY)
                )
            }
        }
    }
}

@Suppress("unused")
private fun nearZero(v: Float): Boolean = abs(v) < 0.001f
