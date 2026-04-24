package com.example.alearning.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.example.alearning.ui.report.ReportScreen
import com.example.alearning.ui.analytics.AnalyticsScreen
import com.example.alearning.ui.roster.RosterScreen
import com.example.alearning.ui.testlibrary.TestLibraryScreen
import com.example.alearning.ui.testing.CreateEventScreen
import com.example.alearning.ui.testing.TestingGridScreen
import com.example.alearning.ui.testing.stopwatch.StopwatchScreen

@Composable
fun ALearningNavGraph(navController: NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route,
        modifier = modifier
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToRoster = { navController.navigate(Screen.Roster.route) },
                onNavigateToTestLibrary = { navController.navigate(Screen.TestLibrary.route) },
                onNavigateToCreateEvent = { navController.navigate(Screen.CreateEvent.route) },
                onNavigateToQuickTest = { navController.navigate(Screen.QuickTest.route) },
                onNavigateToTestingGrid = { eventId, groupId ->
                    navController.navigate(Screen.TestingGrid.createRoute(eventId, groupId))
                },
                onNavigateToLeaderboard = { navController.navigate(Screen.Leaderboard.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) }
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

        composable(Screen.Analytics.route) {
            AnalyticsScreen()
        }

        composable(Screen.Report.route){
            ReportScreen(
                onNavigateToGroupReport = { eventId, groupId ->
                    navController.navigate(Screen.GroupReport.createRoute(eventId, groupId))
                }
            )
        }




        composable(
            route = Screen.TestingGrid.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType },
                navArgument("groupId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            TestingGridScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAthleteReport = { individualId ->
                    navController.navigate(Screen.AthleteReport.createRoute(individualId))
                },
                onNavigateToLeaderboard = { _, _, _ ->
                    navController.navigate(Screen.Leaderboard.route)
                },
                onNavigateToGroupReport = { evId, grId ->
                    navController.navigate(Screen.GroupReport.createRoute(evId, grId))
                },
                onNavigateToStopwatch = { evId, testId, grId, athleteId ->
                    navController.navigate(Screen.Stopwatch.createRoute(evId, testId, grId, athleteId))
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
                onNavigateToLeaderboard = { _, _, _ ->
                    navController.navigate(Screen.Leaderboard.route)
                }
            )
        }




        composable(
            route = Screen.Stopwatch.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType },
                navArgument("fitnessTestId") { type = NavType.StringType },
                navArgument("groupId") { type = NavType.StringType },
                navArgument("athleteId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            StopwatchScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Leaderboard.route) {
            LeaderboardScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
