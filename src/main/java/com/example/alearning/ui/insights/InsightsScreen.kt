package com.example.alearning.ui.insights

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alearning.ui.analytics.AnalyticsContent
import com.example.alearning.ui.components.AppTopBar
import com.example.alearning.ui.report.ReportContent

@Composable
fun InsightsScreen(
    onNavigateToGroupReport: (String, String?) -> Unit,
    onNavigateToAthlete: (String) -> Unit
) {
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = { AppTopBar(title = "Insights") }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Reports", fontWeight = FontWeight.SemiBold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Analytics", fontWeight = FontWeight.SemiBold) }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (selectedTabIndex == 0) {
                    ReportContent(
                        onNavigateToGroupReport = onNavigateToGroupReport,
                        onNavigateToAthlete = onNavigateToAthlete,
                        viewModel = hiltViewModel()
                    )
                } else {
                    AnalyticsContent(
                        viewModel = hiltViewModel()
                    )
                }
            }
        }
    }
}
