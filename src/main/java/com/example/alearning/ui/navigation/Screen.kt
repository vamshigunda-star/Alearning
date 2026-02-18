package com.example.alearning.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Roster : Screen("roster")
    data object TestLibrary : Screen("test_library")
    data object CreateEvent : Screen("create_event")
    data object TestingGrid : Screen("testing_grid/{eventId}/{groupId}") {
        fun createRoute(eventId: String, groupId: String) = "testing_grid/$eventId/$groupId"
    }
    data object StudentReport : Screen("student_report/{studentId}") {
        fun createRoute(studentId: String) = "student_report/$studentId"
    }
}
