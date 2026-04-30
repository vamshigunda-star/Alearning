package com.example.alearning.ui.analytics.tabs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.alearning.domain.model.analytics.GroupProgressPoint
import com.example.alearning.domain.model.analytics.TimePeriod
import com.example.alearning.ui.analytics.AnalyticsUiState
import com.example.alearning.ui.analytics.AnalyticsViewModel
import com.example.alearning.ui.theme.NavyPrimary
import com.example.alearning.ui.theme.PerformanceGreenText
import com.example.alearning.ui.theme.PerformanceRedText
import com.example.alearning.ui.theme.SportOrange
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsTab(state: AnalyticsUiState, viewModel: AnalyticsViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { SectionHeader("Time Range", "Window of group progress") }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        TimePeriod.entries.forEachIndexed { index, period ->
                            SegmentedButton(
                                selected = state.selectedPeriod == period,
                                onClick = { viewModel.updatePeriod(period) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = TimePeriod.entries.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = NavyPrimary,
                                    activeContentColor = Color.White,
                                    inactiveContainerColor = Color.White,
                                    inactiveContentColor = NavyPrimary
                                )
                            ) { Text(period.label) }
                        }
                    }
                }
            }
        }

        if (state.availableGroups.isNotEmpty()) {
            item { SectionHeader("Groups", "Filter the chart") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.availableGroups.forEach { group ->
                        FilterChip(
                            selected = state.selectedGroups.contains(group),
                            onClick = { viewModel.toggleGroup(group) },
                            label = { Text(group, fontWeight = FontWeight.Medium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SportOrange.copy(alpha = 0.18f),
                                selectedLabelColor = SportOrange
                            )
                        )
                    }
                }
            }
        }

        item { SectionHeader("Group Progress", "Average percentile over time") }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (state.groupProgressData.isEmpty() || state.groupProgressData.values.all { it.size < 2 }) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            EmptyHint("Not enough data to chart trends yet.")
                        }
                    } else {
                        MultiLineChart(
                            series = state.groupProgressData,
                            modifier = Modifier.fillMaxWidth().height(220.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        ChartLegend(seriesNames = state.groupProgressData.keys.toList())
                    }
                }
            }
        }

        if (state.groupProgressData.isNotEmpty()) {
            item { SectionHeader("Group Deltas", "Change vs. start of period") }

            items(state.groupProgressData.entries.toList(), key = { it.key }) { (group, points) ->
                GroupDeltaCard(group = group, points = points)
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun MultiLineChart(
    series: Map<String, List<GroupProgressPoint>>,
    modifier: Modifier = Modifier
) {
    val palette = remember {
        listOf(
            SportOrange,
            NavyPrimary,
            Color(0xFF1B5E20),
            Color(0xFF6A1B9A),
            Color(0xFF00838F),
            Color(0xFFB71C1C)
        )
    }
    val gridColor = Color(0xFFE0E0E0)
    val seriesList = series.entries.toList()

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // Y is normalized 0..1
        val yAt = { v: Float -> h * (1f - v.coerceIn(0f, 1f)) }
        // Background bands
        drawRect(Color(0xFF1B5E20).copy(alpha = 0.06f), topLeft = Offset(0f, yAt(1f)), size = Size(w, yAt(0.7f) - yAt(1f)))
        drawRect(NavyPrimary.copy(alpha = 0.04f), topLeft = Offset(0f, yAt(0.7f)), size = Size(w, yAt(0.35f) - yAt(0.7f)))
        // Grid
        listOf(0f, 0.35f, 0.7f, 1f).forEach { v ->
            val y = yAt(v)
            drawLine(gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
        }

        // Find global x range
        val allDates = seriesList.flatMap { it.value }.map { it.date.toEpochDay() }
        if (allDates.isEmpty()) return@Canvas
        val minX = allDates.min()
        val maxX = allDates.max()
        val span = (maxX - minX).coerceAtLeast(1L)

        seriesList.forEachIndexed { idx, (_, points) ->
            if (points.size < 2) return@forEachIndexed
            val color = palette[idx % palette.size]
            val path = Path()
            points.forEachIndexed { i, p ->
                val x = w * ((p.date.toEpochDay() - minX).toFloat() / span.toFloat())
                val y = yAt(p.avgScore)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
            points.forEach { p ->
                val x = w * ((p.date.toEpochDay() - minX).toFloat() / span.toFloat())
                drawCircle(color, radius = 3.dp.toPx(), center = Offset(x, yAt(p.avgScore)))
            }
        }
    }
}

@Composable
private fun ChartLegend(seriesNames: List<String>) {
    val palette = listOf(
        SportOrange,
        NavyPrimary,
        Color(0xFF1B5E20),
        Color(0xFF6A1B9A),
        Color(0xFF00838F),
        Color(0xFFB71C1C)
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        seriesNames.forEachIndexed { idx, name ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(palette[idx % palette.size]))
                Text(name, style = MaterialTheme.typography.labelSmall, color = NavyPrimary)
            }
        }
    }
}

@Composable
private fun GroupDeltaCard(group: String, points: List<GroupProgressPoint>) {
    val current = points.lastOrNull()?.avgScore ?: 0f
    val first = points.firstOrNull()?.avgScore ?: 0f
    val delta = current - first

    val (icon: ImageVector, color: Color, label: String) = when {
        delta > 0.01f -> Triple(Icons.Default.TrendingUp, PerformanceGreenText, "+${(delta * 100).toInt()}%")
        delta < -0.01f -> Triple(Icons.Default.TrendingDown, PerformanceRedText, "−${(abs(delta) * 100).toInt()}%")
        else -> Triple(Icons.Default.Remove, Color.Gray, "0%")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(group, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
                Text(
                    "Avg ${(current * 100).toInt()}% · ${points.size} session${if (points.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(color.copy(alpha = 0.12f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
            }
        }
    }
}
