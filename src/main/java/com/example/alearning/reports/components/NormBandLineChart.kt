package com.example.alearning.reports.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alearning.reports.NormBandsForAge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ChartPoint(val date: Long, val rawScore: Double)

/**
 * Line chart with shaded normative bands.
 *
 * Bands are *behind* the line. Heights of the bands are interpolated against the displayed Y range,
 * which is computed from the union of point scores and band thresholds for stability.
 */
@Composable
fun NormBandLineChart(
    points: List<ChartPoint>,
    bands: List<NormBandsForAge>,
    modifier: Modifier = Modifier.fillMaxWidth().height(220.dp),
    isHigherBetter: Boolean,
    unit: String = "",
    lineColor: Color = Color(0xFF0D47A1),
    superiorColor: Color = Color(0x331B5E20),
    healthyColor: Color = Color(0x330D47A1),
    needsColor: Color = Color(0x33B71C1C)
) {
    val textMeasurer = rememberTextMeasurer()
    val axisLabelStyle = TextStyle(color = Color(0xFF555555), fontSize = 10.sp)
    val dateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas

        val w = size.width
        val h = size.height
        // Reserve space for axis labels: left for Y values, bottom for X dates.
        val padLeft = 44.dp.toPx()
        val padRight = 12.dp.toPx()
        val padTop = 12.dp.toPx()
        val padBottom = 26.dp.toPx()
        val plotW = w - padLeft - padRight
        val plotH = h - padTop - padBottom

        // Y range: union of scores + band thresholds
        val allValues = mutableListOf<Double>()
        allValues += points.map { it.rawScore }
        bands.forEach {
            it.superiorMin?.let(allValues::add)
            it.healthyMin?.let(allValues::add)
            it.needsMax?.let(allValues::add)
        }
        if (allValues.isEmpty()) return@Canvas
        val yMin = allValues.min() - 0.1 * (allValues.max() - allValues.min()).coerceAtLeast(1.0)
        val yMax = allValues.max() + 0.1 * (allValues.max() - allValues.min()).coerceAtLeast(1.0)
        val yRange = (yMax - yMin).coerceAtLeast(0.0001)

        fun toY(v: Double): Float = padTop + plotH * (1f - ((v - yMin) / yRange).toFloat())
        fun toX(idx: Int, total: Int): Float = padLeft + if (total <= 1) plotW / 2f else plotW * idx.toFloat() / (total - 1)

        val avgSuperior = bands.mapNotNull { it.superiorMin }.average().takeIf { !it.isNaN() }
        val avgHealthy = bands.mapNotNull { it.healthyMin }.average().takeIf { !it.isNaN() }

        if (avgSuperior != null && avgHealthy != null) {
            if (isHigherBetter) {
                drawRect(
                    superiorColor,
                    topLeft = Offset(padLeft, padTop),
                    size = Size(plotW, toY(avgSuperior) - padTop)
                )
                drawRect(
                    healthyColor,
                    topLeft = Offset(padLeft, toY(avgSuperior)),
                    size = Size(plotW, toY(avgHealthy) - toY(avgSuperior))
                )
                drawRect(
                    needsColor,
                    topLeft = Offset(padLeft, toY(avgHealthy)),
                    size = Size(plotW, padTop + plotH - toY(avgHealthy))
                )
            } else {
                drawRect(
                    superiorColor,
                    topLeft = Offset(padLeft, toY(avgSuperior)),
                    size = Size(plotW, padTop + plotH - toY(avgSuperior))
                )
                drawRect(
                    healthyColor,
                    topLeft = Offset(padLeft, toY(avgHealthy)),
                    size = Size(plotW, toY(avgSuperior) - toY(avgHealthy))
                )
                drawRect(
                    needsColor,
                    topLeft = Offset(padLeft, padTop),
                    size = Size(plotW, toY(avgHealthy) - padTop)
                )
            }
        }

        // Grid + Y-axis tick labels (4 ticks: top, 2/3, 1/3, bottom)
        val gridColor = Color(0x33000000)
        val tickCount = 4
        for (i in 0 until tickCount) {
            val frac = i / (tickCount - 1).toFloat()
            val gy = padTop + plotH * frac
            drawLine(gridColor, Offset(padLeft, gy), Offset(padLeft + plotW, gy), strokeWidth = 0.5.dp.toPx())

            val value = yMax - (yMax - yMin) * frac
            val label = formatYLabel(value) + if (unit.isNotEmpty()) " $unit" else ""
            val measured = textMeasurer.measure(label, axisLabelStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = padLeft - measured.size.width - 4.dp.toPx(),
                    y = gy - measured.size.height / 2f
                )
            )
        }

        // Y-axis line
        drawLine(
            Color(0x66000000),
            Offset(padLeft, padTop),
            Offset(padLeft, padTop + plotH),
            strokeWidth = 0.8.dp.toPx()
        )
        // X-axis line
        drawLine(
            Color(0x66000000),
            Offset(padLeft, padTop + plotH),
            Offset(padLeft + plotW, padTop + plotH),
            strokeWidth = 0.8.dp.toPx()
        )

        // X-axis date labels (first, middle if 3+, last)
        val xLabelIndices: List<Int> = when {
            points.size <= 1 -> listOf(0)
            points.size == 2 -> listOf(0, 1)
            else -> listOf(0, points.size / 2, points.size - 1)
        }
        for (idx in xLabelIndices) {
            val label = dateFormatter.format(Date(points[idx].date))
            val measured = textMeasurer.measure(label, axisLabelStyle)
            val rawX = toX(idx, points.size) - measured.size.width / 2f
            // Clamp so first/last labels don't overflow horizontally
            val x = rawX.coerceIn(
                padLeft - measured.size.width / 4f,
                padLeft + plotW - measured.size.width + measured.size.width / 4f
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    x = x,
                    y = padTop + plotH + 6.dp.toPx()
                )
            )
        }

        // Line
        val path = Path()
        points.forEachIndexed { i, p ->
            val x = toX(i, points.size)
            val y = toY(p.rawScore)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx()))

        // Dots
        points.forEachIndexed { i, p ->
            val x = toX(i, points.size)
            val y = toY(p.rawScore)
            drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
            drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(x, y))
        }
    }
}

private fun formatYLabel(value: Double): String =
    if (kotlin.math.abs(value) >= 100 || value % 1.0 == 0.0) value.toInt().toString()
    else String.format("%.1f", value)
