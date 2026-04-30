package com.example.alearning.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Roster : Screen("roster")
    data object Athletes : Screen("athletes")
    data object Insights : Screen("insights")
    data object TestLibrary : Screen("test_library")
    data object CreateEvent : Screen("create_event")
    data object QuickTest : Screen("quick_test?athleteId={athleteId}&testIds={testIds}") {
        fun createRoute(athleteId: String? = null, testIds: List<String> = emptyList()): String {
            val params = buildList {
                if (athleteId != null) add("athleteId=$athleteId")
                if (testIds.isNotEmpty()) add("testIds=${testIds.joinToString(",")}")
            }
            return "quick_test" + if (params.isEmpty()) "" else "?" + params.joinToString("&")
        }
    }
    data object Report : Screen("reports")
    data object Analytics : Screen("analytics")
    data object Leaderboard : Screen("leaderboard/{eventId}/{groupId}/{mode}") {
        fun createRoute(eventId: String, groupId: String, mode: String) =
            "leaderboard/$eventId/$groupId/$mode"
    }

    data object TestingGrid : Screen("testing_grid/{eventId}/{groupId}") {
        fun createRoute(eventId: String, groupId: String) = "testing_grid/$eventId/$groupId"
    }

    data object Stopwatch : Screen("stopwatch/{eventId}/{fitnessTestId}/{groupId}?athleteId={athleteId}") {
        fun createRoute(eventId: String, fitnessTestId: String, groupId: String, athleteId: String? = null) =
            "stopwatch/$eventId/$fitnessTestId/$groupId" + if (athleteId != null) "?athleteId=$athleteId" else ""
    }

    // Reports & Results layer
    data object GroupOverview : Screen("group/{groupId}") {
        fun createRoute(groupId: String) = "group/$groupId"
    }

    data object SessionReport : Screen("group/{groupId}/session/{sessionId}") {
        fun createRoute(groupId: String, sessionId: String) = "group/$groupId/session/$sessionId"
    }

    data object AthleteDashboard : Screen("athlete/{athleteId}?contextSessionId={contextSessionId}") {
        fun createRoute(athleteId: String, contextSessionId: String? = null) =
            "athlete/$athleteId" + if (contextSessionId != null) "?contextSessionId=$contextSessionId" else ""
    }

    data object AthleteTestDetail : Screen("athlete/{athleteId}/test/{testId}?contextSessionId={contextSessionId}") {
        fun createRoute(athleteId: String, testId: String, contextSessionId: String? = null) =
            "athlete/$athleteId/test/$testId" +
                if (contextSessionId != null) "?contextSessionId=$contextSessionId" else ""
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

    data object Roster : BottomNavItem(
        route = Screen.Roster.route,
        title = "Roster",
        icon = Icons.Default.Groups
    )

    data object Athletes : BottomNavItem(
        route = Screen.Athletes.route,
        title = "Athletes",
        icon = Icons.Default.Person
    )

    data object Reports : BottomNavItem(
        route = Screen.Report.route,
        title = "Reports",
        icon = Icons.AutoMirrored.Filled.Assignment
    )

    data object Analytics : BottomNavItem(
        route = Screen.Analytics.route,
        title = "Analytics",
        icon = Icons.AutoMirrored.Filled.TrendingUp
    )
}
