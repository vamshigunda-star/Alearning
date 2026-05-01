package com.example.alearning.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.alearning.ui.athlete.AthleteTestDetailScreen
import com.example.alearning.ui.athlete.AthleteDashboardScreen
import com.example.alearning.ui.athletes.AthletesScreen
import com.example.alearning.ui.dashboard.DashboardScreen
import com.example.alearning.ui.groupoverview.GroupOverviewScreen

import com.example.alearning.ui.leaderboard.LeaderboardScreen
import com.example.alearning.ui.quicktest.QuickTestScreen
import com.example.alearning.ui.roster.RosterScreen
import com.example.alearning.ui.session.SessionReportScreen
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
                onNavigateToRoster = { navController.navigate(BottomNavItem.Roster.route) },
                onNavigateToAnalytics = { navController.navigate(BottomNavItem.Analytics.route) },
                onNavigateToTestLibrary = { navController.navigate(Screen.TestLibrary.route) },
                onNavigateToCreateEvent = { navController.navigate(Screen.CreateEvent.route) },
                onNavigateToQuickTest = { navController.navigate(Screen.QuickTest.createRoute()) },
                onNavigateToTestingGrid = { eventId, groupId ->
                    navController.navigate(Screen.TestingGrid.createRoute(eventId, groupId))
                },
                onNavigateToLeaderboard = {
                    // Navigate to a default or global leaderboard if applicable, 
                    // or this could be handled by passing specific IDs if the UI provided them.
                },
                onNavigateToReports = { navController.navigate(BottomNavItem.Reports.route) }
            )
        }

        composable(Screen.Roster.route) {
            RosterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAthleteReport = { athleteId ->
                    navController.navigate(Screen.AthleteDashboard.createRoute(athleteId))
                }
            )
        }

        composable(Screen.Athletes.route) {
            AthletesScreen(
                onNavigateToAthleteReport = { athleteId ->
                    navController.navigate(Screen.AthleteDashboard.createRoute(athleteId))
                }
            )
        }

        composable(Screen.TestLibrary.route) {
            TestLibraryScreen(onNavigateBack = { navController.popBackStack() })
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
            route = Screen.QuickTest.route,
            arguments = listOf(
                navArgument("athleteId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("testIds") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            QuickTestScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ───── Reports & Results layer ─────

        composable(Screen.Report.route) {
            com.example.alearning.ui.report.ReportScreen(
                onNavigateToGroup = { groupId ->
                    navController.navigate(Screen.GroupOverview.createRoute(groupId))
                },
                onNavigateToSession = { groupId, sessionId ->
                    navController.navigate(Screen.SessionReport.createRoute(groupId, sessionId))
                },
                onNavigateToAthlete = { athleteId ->
                    navController.navigate(Screen.AthleteDashboard.createRoute(athleteId))
                }
            )
        }

        composable(Screen.Analytics.route) {
            com.example.alearning.ui.analytics.AnalyticsScreen()
        }

        composable(
            route = Screen.GroupOverview.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) {
            GroupOverviewScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSession = { groupId, sessionId ->
                    navController.navigate(Screen.SessionReport.createRoute(groupId, sessionId))
                },
                onNavigateToCreateSession = { navController.navigate(Screen.CreateEvent.route) }
            )
        }

        composable(
            route = Screen.SessionReport.route,
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("sessionId") { type = NavType.StringType }
            )
        ) {
            SessionReportScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAthlete = { individualId, sessionId ->
                    navController.navigate(Screen.AthleteDashboard.createRoute(individualId, sessionId))
                },
                onResumeTesting = { eventId, groupId ->
                    navController.navigate(Screen.TestingGrid.createRoute(eventId, groupId))
                }
            )
        }

        composable(
            route = Screen.AthleteDashboard.route,
            arguments = listOf(
                navArgument("athleteId") { type = NavType.StringType },
                navArgument("contextSessionId") { type = NavType.StringType; nullable = true }
            )
        ) {
            AthleteDashboardScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTest = { athleteId, testId, contextSessionId ->
                    navController.navigate(Screen.AthleteTestDetail.createRoute(athleteId, testId, contextSessionId))
                },
                onStartQuickTest = { athleteId, testIds ->
                    navController.navigate(Screen.QuickTest.createRoute(athleteId, testIds))
                }
            )
        }

        composable(
            route = Screen.AthleteTestDetail.route,
            arguments = listOf(
                navArgument("athleteId") { type = NavType.StringType },
                navArgument("testId") { type = NavType.StringType },
                navArgument("contextSessionId") { type = NavType.StringType; nullable = true }
            )
        ) {
            AthleteTestDetailScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ───── Live Testing layer ─────

        composable(
            route = Screen.TestingGrid.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType },
                navArgument("groupId") { type = NavType.StringType }
            )
        ) {
            TestingGridScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToStopwatch = { eventId, testId, groupId, athleteId ->
                    navController.navigate(Screen.Stopwatch.createRoute(eventId, testId, groupId, athleteId))
                },
                onNavigateToLeaderboard = { eventId, groupId, mode ->
                    navController.navigate(Screen.Leaderboard.createRoute(eventId, groupId, mode))
                },
                onNavigateToAthleteReport = { athleteId ->
                    navController.navigate(Screen.AthleteDashboard.createRoute(athleteId))
                },
                onNavigateToGroupReport = { eventId, groupId ->
                    navController.navigate(Screen.SessionReport.createRoute(groupId, eventId))
                }
            )
        }

        composable(
            route = Screen.Stopwatch.route,
            arguments = listOf(
                navArgument("eventId") { type = NavType.StringType },
                navArgument("fitnessTestId") { type = NavType.StringType },
                navArgument("groupId") { type = NavType.StringType },
                navArgument("athleteId") { type = NavType.StringType; nullable = true }
            )
        ) {
            StopwatchScreen(
                onNavigateBack = { navController.popBackStack() }
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
