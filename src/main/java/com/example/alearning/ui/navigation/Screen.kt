package com.example.alearning.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Groups
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Roster : Screen("roster")
    data object TestLibrary : Screen("test_library")
    data object CreateEvent : Screen("create_event")
    data object QuickTest : Screen("quick_test")
    data object Report : Screen("report")
    data object Analytics : Screen("analytics")

    data object TestingGrid : Screen("testing_grid/{eventId}/{groupId}") {
        fun createRoute(eventId: String, groupId: String) = "testing_grid/$eventId/$groupId"
    }

    data object AthleteReport : Screen("athlete_report/{individualId}") {
        fun createRoute(individualId: String) = "athlete_report/$individualId"
    }

    data object GroupReport : Screen("group_report/{eventId}/{groupId}") {
        fun createRoute(eventId: String, groupId: String) = "group_report/$eventId/$groupId"
    }

    data object Leaderboard : Screen("leaderboard")

    data object Stopwatch : Screen("stopwatch/{eventId}/{fitnessTestId}/{groupId}") {
        fun createRoute(eventId: String, fitnessTestId: String, groupId: String) =
            "stopwatch/$eventId/$fitnessTestId/$groupId"
    }
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(
        route = Screen.Dashboard.route,
        title = "Home",
        icon = Icons.Default.Home
    )

    data object Athletes : BottomNavItem(
        route = Screen.Roster.route,
        title = "Athletes",
        icon = Icons.Default.Groups
    )

    data object Reports : BottomNavItem(
        route = Screen.Report.route, // Placeholder for now
        title = "Reports",
        icon = Icons.AutoMirrored.Filled.Assignment
    )

    data object Analytics : BottomNavItem(
        route = Screen.Analytics.route, // Placeholder for now
        title = "Analytics",
        icon = Icons.AutoMirrored.Filled.TrendingUp
    )
}
