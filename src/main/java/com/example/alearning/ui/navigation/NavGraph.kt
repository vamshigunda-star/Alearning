package com.example.alearning.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.alearning.ui.dashboard.DashboardScreen
import com.example.alearning.ui.groupreport.GroupReportScreen
import com.example.alearning.ui.leaderboard.LeaderboardScreen
import com.example.alearning.ui.quicktest.QuickTestScreen
import com.example.alearning.ui.report.AthleteReportScreen
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
                onNavigateToQuickTest = { navController.navigate(Screen.QuickTest.route) },
                onNavigateToTestingGrid = { eventId, groupId ->
                    navController.navigate(Screen.TestingGrid.createRoute(eventId, groupId))
                }
            )
        }

        composable(Screen.Roster.route) {
            RosterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAthleteReport = { individualId ->
                    navController.navigate(Screen.AthleteReport.createRoute(individualId))
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

        composable(Screen.QuickTest.route) {
            QuickTestScreen(
                onNavigateBack = { navController.popBackStack() }
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
                onNavigateToAthleteReport = { individualId ->
                    navController.navigate(Screen.AthleteReport.createRoute(individualId))
                },
                onNavigateToLeaderboard = { eventId, groupId, mode ->
                    navController.navigate(Screen.Leaderboard.createRoute(eventId, groupId, mode))
                },
                onNavigateToGroupReport = { eventId, groupId ->
                    navController.navigate(Screen.GroupReport.createRoute(eventId, groupId))
                }
            )
        }

        composable(
            route = Screen.AthleteReport.route,
            arguments = listOf(
                navArgument("individualId") { type = NavType.StringType }
            )
        ) {
            AthleteReportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.GroupReport.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType },
                navArgument("groupId") { type = NavType.StringType }
            )
        ) {
            GroupReportScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAthleteReport = { individualId ->
                    navController.navigate(Screen.AthleteReport.createRoute(individualId))
                },
                onNavigateToLeaderboard = { eventId, groupId, mode ->
                    navController.navigate(Screen.Leaderboard.createRoute(eventId, groupId, mode))
                }
            )
        }

        composable(
            route = Screen.Leaderboard.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType },
                navArgument("groupId") { type = NavType.StringType },
                navArgument("mode") { type = NavType.StringType }
            )
        ) {
            LeaderboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
