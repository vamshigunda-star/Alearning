package com.example.alearning.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.alearning.ui.aicoach.AiCoachScreen
import com.example.alearning.ui.athlete.AthleteDashboardScreen
import com.example.alearning.ui.athletes.AthletesScreen
import com.example.alearning.ui.auth.AuthGateState
import com.example.alearning.ui.auth.AuthGateViewModel
import com.example.alearning.ui.auth.reset.ResetPasswordScreen
import com.example.alearning.ui.auth.signin.SignInScreen
import com.example.alearning.ui.auth.signup.SignUpScreen
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
    val authGateViewModel: AuthGateViewModel = hiltViewModel()
    val authGateState by authGateViewModel.state.collectAsState()

    // While the auth gate is resolving, show a full-screen spinner so we never
    // briefly flash the wrong destination.
    if (authGateState == AuthGateState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = when (authGateState) {
        AuthGateState.Authenticated -> Screen.Dashboard.route
        AuthGateState.UnauthenticatedHasUsers -> Screen.SignIn.route
        AuthGateState.UnauthenticatedNoUsers -> Screen.SignUp.route
        AuthGateState.Loading -> Screen.SignIn.route // unreachable — handled above
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // ───── Auth screens ─────

        composable(Screen.SignIn.route) {
            SignInScreen(
                onSignInSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSignUp = {
                    navController.navigate(Screen.SignUp.route)
                },
                onNavigateToResetPassword = {
                    navController.navigate(Screen.ResetPassword.route)
                }
            )
        }

        composable(Screen.SignUp.route) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSignIn = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ResetPassword.route) {
            ResetPasswordScreen(
                onBack = { navController.popBackStack() },
                onResetSuccess = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(Screen.ResetPassword.route) { inclusive = true }
                    }
                }
            )
        }

        // ───── Main app screens ─────

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToRoster = { navController.navigate(BottomNavItem.Roster.route) },
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
                onNavigateToReports = { navController.navigate(Screen.Report.route) },
                onNavigateToSignIn = {
                    navController.navigate(Screen.SignIn.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
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
                onNavigateToAthleteReport = { athleteId: String ->
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
                },
                onNavigateToAiCoach = { navController.navigate(Screen.AiCoach.route) }
            )
        }

        composable(
            route = Screen.AthleteDashboard.route,
            arguments = listOf(
                navArgument("athleteId") { type = NavType.StringType },
                navArgument("contextSessionId") { type = NavType.StringType; nullable = true }
            )
        ) { backStackEntry ->
            val athleteId = backStackEntry.arguments?.getString("athleteId") ?: ""
            val contextSessionId = backStackEntry.arguments?.getString("contextSessionId")
            AthleteDashboardScreen(
                athleteId = athleteId,
                contextSessionId = contextSessionId,
                onNavigateBack = { navController.popBackStack() },
                onStartQuickTest = { aid, testIds ->
                    navController.navigate(Screen.QuickTest.createRoute(aid, testIds))
                },
                onNavigateToAiCoach = { navController.navigate(Screen.AiCoach.route) }
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

        composable(Screen.AiCoach.route) {
            AiCoachScreen(viewModel = hiltViewModel())
        }

    }
}
