package com.example.alearning.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Roster : Screen("roster")
    data object TestLibrary : Screen("test_library")
    data object CreateEvent : Screen("create_event")
    data object QuickTest : Screen("quick_test")

    data object TestingGrid : Screen("testing_grid/{eventId}/{groupId}") {
        fun createRoute(eventId: String, groupId: String) = "testing_grid/$eventId/$groupId"
    }

    data object AthleteReport : Screen("athlete_report/{individualId}") {
        fun createRoute(individualId: String) = "athlete_report/$individualId"
    }

    data object GroupReport : Screen("group_report/{eventId}/{groupId}") {
        fun createRoute(eventId: String, groupId: String) = "group_report/$eventId/$groupId"
    }

    data object Leaderboard : Screen("leaderboard/{eventId}/{groupId}/{mode}") {
        fun createRoute(eventId: String, groupId: String, mode: String) =
            "leaderboard/$eventId/$groupId/$mode"
    }
}
