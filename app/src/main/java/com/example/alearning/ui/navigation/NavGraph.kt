package com.example.alearning.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.alearning.ui.dashboard.DashboardScreen
import com.example.alearning.ui.report.StudentReportScreen
import com.example.alearning.ui.roster.RosterScreen
import com.example.alearning.ui.testlibrary.TestLibraryScreen
import com.example.alearning.ui.testing.CreateEventScreen
import com.example.alearning.ui.testing.TestingGridScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToRoster = { navController.navigate(Screen.Roster.route) },
                onNavigateToTestLibrary = { navController.navigate(Screen.TestLibrary.route) },
                onNavigateToCreateEvent = { navController.navigate(Screen.CreateEvent.route) },
                onNavigateToTestingGrid = { eventId, groupId ->
                    navController.navigate(Screen.TestingGrid.createRoute(eventId, groupId))
                }
            )
        }

        composable(Screen.Roster.route) {
            RosterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStudentReport = { studentId ->
                    navController.navigate(Screen.StudentReport.createRoute(studentId))
                }
            )
        }

        composable(Screen.TestLibrary.route) {
            TestLibraryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CreateEvent.route) {
            CreateEventScreen(
                onNavigateBack = { navController.popBackStack() },
                onEventCreated = { eventId, groupId ->
                    navController.navigate(Screen.TestingGrid.createRoute(eventId, groupId)) {
                        popUpTo(Screen.Dashboard.route)
                    }
                }
            )
        }

        composable(
            route = Screen.TestingGrid.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType },
                navArgument("groupId") { type = NavType.StringType }
            )
        ) {
            TestingGridScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStudentReport = { studentId ->
                    navController.navigate(Screen.StudentReport.createRoute(studentId))
                }
            )
        }

        composable(
            route = Screen.StudentReport.route,
            arguments = listOf(
                navArgument("studentId") { type = NavType.StringType }
            )
        ) {
            StudentReportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
