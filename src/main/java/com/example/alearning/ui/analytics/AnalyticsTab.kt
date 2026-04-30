package com.example.alearning.ui.analytics

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

enum class AnalyticsTab(val index: Int, val label: String, val icon: ImageVector) {
    OVERVIEW(0, "Overview", Icons.Default.Dashboard),
    TRENDS(1, "Trends", Icons.Default.TrendingUp),
    REMEDIATION(2, "Remediation", Icons.Default.Warning),
    INSIGHTS(3, "Insights", Icons.Default.Lightbulb)
}
