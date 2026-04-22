package com.example.alearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.RadioButtonDefaults.colors
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.alearning.ui.navigation.BottomNavItem
import com.example.alearning.ui.navigation.ALearningNavGraph
import com.example.alearning.ui.theme.AlearningTheme
import com.example.alearning.ui.theme.SportOrange
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlearningTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = {
                        ALearningBottomBar(navController = navController)
                    }
                ) { innerPadding ->
                    ALearningNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun ALearningBottomBar(navController: androidx.navigation.NavController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Athletes,
        BottomNavItem.Reports,
        BottomNavItem.Analytics
    )

    NavigationBar(
        containerColor = Color.White,
        contentColor = Color.Gray
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = selected,
                onClick = {
                    // FIX: Only navigate if we aren't already on this tab
                    if (currentDestination?.route != screen.route) {
                        navController.navigate(screen.route) {
                            // FIX: Clear everything up to the start destination
                            popUpTo(navController.graph.findStartDestination().id) {
                                // REMOVED: saveState = true
                            }
                            // Avoid multiple copies of the same destination
                            launchSingleTop = true
                            // REMOVED: restoreState = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = SportOrange,
                    selectedTextColor = SportOrange,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent // Crucial for the desired design
                )
            )

        }


    }
}
