package com.example.alearning.ui.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.alearning.ui.analytics.tabs.InsightsTab
import com.example.alearning.ui.analytics.tabs.OverviewTab
import com.example.alearning.ui.analytics.tabs.RemediationTab
import com.example.alearning.ui.analytics.tabs.TrendsTab
import com.example.alearning.ui.theme.BackgroundLight
import com.example.alearning.ui.theme.NavyPrimary
import com.example.alearning.ui.theme.NavyVariant
import com.example.alearning.ui.theme.SportOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    navController: NavController? = null,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    Scaffold(
        containerColor = BackgroundLight,
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(NavyPrimary, NavyVariant))
                        )
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SportOrange.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                tint = SportOrange
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "Analytics",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            Text(
                                "Roster-wide performance insights",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                AnalyticsTabBar(
                    selectedTab = selectedTab,
                    onTabSelected = viewModel::selectTab
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(BackgroundLight)) {
            AnalyticsContent(navController, viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AnalyticsTabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    SecondaryScrollableTabRow(
        selectedTabIndex = selectedTab,
        containerColor = NavyVariant,
        contentColor = Color.White,
        edgePadding = 12.dp,
        indicator = {
            TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(selectedTab),
                color = SportOrange,
                height = 3.dp
            )
        },
        divider = {}
    ) {
        AnalyticsTab.entries.forEach { tab ->
            val selected = selectedTab == tab.index
            Tab(
                selected = selected,
                onClick = { onTabSelected(tab.index) },
                text = {
                    Text(
                        tab.label,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                selectedContentColor = SportOrange,
                unselectedContentColor = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun AnalyticsContent(
    navController: NavController? = null,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (selectedTab) {
            0 -> OverviewTab(state)
            1 -> TrendsTab(state, viewModel)
            2 -> RemediationTab(state, navController, viewModel)
            3 -> InsightsTab(state)
        }
    }
}
