package com.example.alearning.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import com.example.alearning.ui.athlete.AthleteTestDetailScreen
import com.example.alearning.ui.auth.AuthGateState
import com.example.alearning.ui.auth.AuthGateViewModel
import com.example.alearning.ui.auth.reset.ResetPasswordScreen
import com.example.alearning.ui.auth.signin.SignInScreen
import com.example.alearning.ui.auth.signup.SignUpScreen
import com.example.alearning.ui.dashboard.DashboardScreen
import com.example.alearning.ui.groupoverview.GroupOverviewScreen
import com.example.alearning.ui.leaderboard.LeaderboardScreen
import com.example.alearning.ui.quicktest.QuickTestScreen
import com.example.alearning.ui.report.ReportScreen
import com.example.alearning.ui.roster.RosterScreen
import com.example.alearning.ui.session.SessionReportScreen
import com.example.alearning.ui.settings.SettingsScreen
import com.example.alearning.ui.testing.CreateEventScreen
import com.example.alearning.ui.testing.TestingGridScreen
import com.example.alearning.ui.testing.stopwatch.StopwatchScreen
import com.example.alearning.ui.testlibrary.TestLibraryScreen

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
        modifier = modifier,
        enterTransition = { slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth / 4 } + fadeIn(animationSpec = tween(220)) },
        exitTransition = { slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth / 4 } + fadeOut(animationSpec = tween(220)) },
        popEnterTransition = { slideInHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth / 4 } + fadeIn(animationSpec = tween(220)) },
        popExitTransition = { slideOutHorizontally(animationSpec = tween(220, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth / 4 } + fadeOut(animationSpec = tween(220)) }
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
                onNavigateToLeaderboard = { eventId, groupId, mode ->
                    navController.navigate(Screen.Leaderboard.createRoute(eventId, groupId, mode))
                },
                onNavigateToReports = { navController.navigate(Screen.Report.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
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
                },
                onNavigateToTest = { aId, tId, cId ->
                    navController.navigate(Screen.AthleteTestDetail.createRoute(aId, tId, cId))
                }
            )
        }



        composable(Screen.TestLibrary.route) {

            TestLibraryScreen(onNavigateBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.CreateEvent.route,
            enterTransition = { slideInHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth / 4 } + fadeIn(animationSpec = tween(200)) },
            exitTransition = { slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth / 4 } + fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { slideInHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth / 4 } + fadeIn(animationSpec = tween(200)) },
            popExitTransition = { slideOutHorizontally(animationSpec = tween(200, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth / 4 } + fadeOut(animationSpec = tween(200)) }
        ) {

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
                navArgument("testIds") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("eventId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) {
            QuickTestScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // ───── Reports & Results layer ─────

        composable(Screen.Report.route) {

            ReportScreen(
                onNavigateToGroup = { groupId ->
                    navController.navigate(Screen.GroupOverview.createRoute(groupId))
                },
                onNavigateToSession = { groupId, sessionId ->
                    navController.navigate(Screen.SessionReport.createRoute(groupId, sessionId))
                },
                onNavigateToAthlete = { athleteId ->
                    navController.navigate(Screen.AthleteDashboard.createRoute(athleteId))
                },
                onNavigateToTest = { athleteId, testId ->
                    navController.navigate(Screen.AthleteTestDetail.createRoute(athleteId, testId, null))
                },
                onNavigateToAiCoach = { contextString -> 
                    navController.navigate(Screen.AiCoach.createRoute(contextString)) 
                },
                onStartQuickTest = { athleteId, testIds ->
                    navController.navigate(Screen.QuickTest.createRoute(athleteId = athleteId, testIds = testIds))
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
                onResumeTesting = { eventId, groupId, athleteId, testIds ->
                    if (groupId != null) {
                        navController.navigate(Screen.TestingGrid.createRoute(eventId, groupId = groupId))
                    } else {
                        navController.navigate(Screen.QuickTest.createRoute(athleteId = athleteId, testIds = testIds ?: emptyList(), eventId = eventId))
                    }
                },
                onNavigateToAiCoach = { contextString -> navController.navigate(Screen.AiCoach.createRoute(contextString)) }
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
                onNavigateToTest = { aId, tId, cId ->
                    navController.navigate(Screen.AthleteTestDetail.createRoute(aId, tId, cId))
                },
                onStartQuickTest = { aId, testIds ->
                    navController.navigate(Screen.QuickTest.createRoute(aId, testIds))
                },
                onNavigateToAiCoach = { contextData ->
                    if (contextData != null) {
                        navController.currentBackStackEntry?.savedStateHandle?.set("ai_coach_context", contextData)
                    }
                    navController.navigate(Screen.AiCoach.route)
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
        ) { backStackEntry ->
            val athleteId = backStackEntry.arguments?.getString("athleteId") ?: ""
            val testId = backStackEntry.arguments?.getString("testId") ?: ""
            val contextSessionId = backStackEntry.arguments?.getString("contextSessionId")
            AthleteTestDetailScreen(
                athleteId = athleteId,
                testId = testId,
                contextSessionId = contextSessionId,
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

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AiCoach.route,
            arguments = listOf(navArgument("context") { nullable = true; type = NavType.StringType })
        ) {
            AiCoachScreen(
                viewModel = hiltViewModel(),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
