package com.example.alearning.ui.analytics.tabs

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alearning.domain.model.analytics.AnalyticsAction
import com.example.alearning.domain.usecase.analytics.IndividualEventSummary
import com.example.alearning.domain.usecase.analytics.LongitudinalTrend
import com.example.alearning.domain.usecase.analytics.PerformanceZone
import com.example.alearning.domain.usecase.analytics.TrajectoryTrend
import com.example.alearning.domain.usecase.testing.AthleteRadarData
import com.example.alearning.ui.analytics.AnalyticsUiState
import com.example.alearning.ui.analytics.AnalyticsViewModel
import com.example.alearning.ui.theme.NavyPrimary
import com.example.alearning.ui.theme.NavyVariant
import com.example.alearning.ui.theme.SportOrange
import com.example.alearning.ui.theme.PerformanceGreen
import com.example.alearning.ui.theme.PerformanceYellow
import com.example.alearning.ui.theme.PerformanceRed
import com.example.alearning.ui.theme.PerformanceGreenText
import com.example.alearning.ui.theme.PerformanceYellowText
import com.example.alearning.ui.theme.PerformanceRedText
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualTab(state: AnalyticsUiState, viewModel: AnalyticsViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Global Athlete Selector
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            val selectedName = state.availableIndividuals.find { it.id == state.selectedIndividualId }?.fullName ?: "Select Athlete"
            OutlinedTextField(
                value = selectedName,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                    focusedBorderColor = SportOrange,
                    focusedLabelColor = SportOrange
                ),
                label = { Text("Active Athlete") }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                state.availableIndividuals.forEach { individual ->
                    DropdownMenuItem(
                        text = { Text(individual.fullName, fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            viewModel.onAction(AnalyticsAction.SelectIndividual(individual.id))
                            expanded = false
                        }
                    )
                }
            }
        }

        Crossfade(
            targetState = state.individualAnalytics,
            animationSpec = tween(500),
            label = "individual_data_crossfade"
        ) { analytics ->
            if (analytics == null) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = SportOrange)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Module 1: Single Testing Event Summary
                    if (analytics.latestEventSummary.isNotEmpty()) {
                        Text("Latest Test Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            analytics.latestEventSummary.take(2).forEach { summary ->
                                EventSummaryCard(summary = summary, modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Module 2: Longitudinal Tracking
                    if (analytics.longitudinalTrends.isNotEmpty()) {
                        Text("Longitudinal Trends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = NavyVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                analytics.longitudinalTrends.forEach { trend ->
                                    TrendRow(trend = trend)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }

                    // Module 3: Individual Spider/Radar Chart
                    if (analytics.radarData != null) {
                        Text("Performance Radar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Card(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            colors = CardDefaults.cardColors(containerColor = NavyVariant),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                IndividualRadarChart(radarData = analytics.radarData)
                            }
                        }
                    }

                    // Module 4: AI Intervention Studio
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = NavyPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = SportOrange)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AI Intervention Studio", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            AnimatedVisibility(visible = state.aiPrescription != null) {
                                Text(
                                    text = state.aiPrescription ?: "",
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                            Button(
                                onClick = { viewModel.onAction(AnalyticsAction.GenerateAIPrescription) },
                                colors = ButtonDefaults.buttonColors(containerColor = SportOrange),
                                enabled = !state.isAIGenerating
                            ) {
                                if (state.isAIGenerating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Generating...")
                                } else {
                                    Text("Generate Prescription")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EventSummaryCard(summary: IndividualEventSummary, modifier: Modifier = Modifier) {
    val zoneColor = when (summary.zone) {
        PerformanceZone.GREEN -> PerformanceGreenText
        PerformanceZone.YELLOW -> PerformanceYellowText
        PerformanceZone.RED -> PerformanceRedText
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = NavyVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(summary.testName, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.1f", summary.rawScore),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(summary.unit, color = Color.White.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(zoneColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${summary.percentile}th %ile",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TrendRow(trend: LongitudinalTrend) {
    val trendIcon = when (trend.trend) {
        TrajectoryTrend.IMPROVING -> "📈"
        TrajectoryTrend.PLATEAU -> "⏸️"
        TrajectoryTrend.DECLINE -> "📉"
        TrajectoryTrend.INSUFFICIENT_DATA -> "➖"
    }
    val trendColor = when (trend.trend) {
        TrajectoryTrend.IMPROVING -> PerformanceGreenText
        TrajectoryTrend.PLATEAU -> PerformanceYellowText
        TrajectoryTrend.DECLINE -> PerformanceRedText
        TrajectoryTrend.INSUFFICIENT_DATA -> Color.Gray
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(trendIcon, modifier = Modifier.padding(end = 8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(trend.categoryName, color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                if (trend.history.size > 1) {
                    val maxScore = 100f
                    val path = Path()
                    val widthPerPoint = size.width / (trend.history.size - 1)
                    trend.history.forEachIndexed { index, point ->
                        val x = index * widthPerPoint
                        val y = size.height - (point.score / maxScore * size.height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        drawCircle(color = SportOrange, radius = 4.dp.toPx(), center = Offset(x, y))
                    }
                    drawPath(
                        path = path,
                        color = trendColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }
        }
    }
}

@Composable
fun IndividualRadarChart(radarData: AthleteRadarData) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) / 2f * 0.8f // Leave room for labels
        
        val axesCount = radarData.axisScores.size
        if (axesCount < 3) return@Canvas
        
        val anglePerAxis = (2 * Math.PI) / axesCount
        
        // Draw concentric zones (Red -> Yellow -> Green)
        drawConcentricPolygon(axesCount, center, radius * 1.0f, PerformanceGreen.copy(alpha = 0.2f)) // 100%
        drawConcentricPolygon(axesCount, center, radius * 0.59f, PerformanceYellow.copy(alpha = 0.3f)) // 59% (Yellow)
        drawConcentricPolygon(axesCount, center, radius * 0.29f, PerformanceRed.copy(alpha = 0.4f)) // 29% (Red)
        
        // Draw Axis Lines
        for (i in 0 until axesCount) {
            val angle = i * anglePerAxis - Math.PI / 2
            val x = center.x + (radius * cos(angle)).toFloat()
            val y = center.y + (radius * sin(angle)).toFloat()
            drawLine(
                color = Color.White.copy(alpha = 0.2f),
                start = center,
                end = Offset(x, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Draw Performance Polygon
        val performancePath = Path()
        radarData.axisScores.forEachIndexed { i, axisScore ->
            val angle = i * anglePerAxis - Math.PI / 2
            val scoreRadius = radius * axisScore.normalizedScore
            val x = center.x + (scoreRadius * cos(angle)).toFloat()
            val y = center.y + (scoreRadius * sin(angle)).toFloat()
            
            if (i == 0) performancePath.moveTo(x, y)
            else performancePath.lineTo(x, y)
        }
        performancePath.close()
        
        drawPath(
            path = performancePath,
            color = SportOrange.copy(alpha = 0.6f)
        )
        drawPath(
            path = performancePath,
            color = SportOrange,
            style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
        )
        
        // Draw Labels
        val textPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 12.dp.toPx()
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        
        radarData.axisScores.forEachIndexed { i, axisScore ->
            val angle = i * anglePerAxis - Math.PI / 2
            val labelRadius = radius * 1.15f
            val x = center.x + (labelRadius * cos(angle)).toFloat()
            val y = center.y + (labelRadius * sin(angle)).toFloat() + (textPaint.textSize / 3) // Vertical center adjustment
            
            drawContext.canvas.nativeCanvas.drawText(
                axisScore.axis.name,
                x,
                y,
                textPaint
            )
        }
    }
}

private fun DrawScope.drawConcentricPolygon(axesCount: Int, center: Offset, radius: Float, color: Color) {
    val path = Path()
    val anglePerAxis = (2 * Math.PI) / axesCount
    for (i in 0 until axesCount) {
        val angle = i * anglePerAxis - Math.PI / 2
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path = path, color = color)
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.3f),
        style = Stroke(width = 1.dp.toPx())
    )
}
