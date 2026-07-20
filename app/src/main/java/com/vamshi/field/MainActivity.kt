package com.vamshi.field

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.vamshi.field.ui.navigation.ALearningNavGraph
import com.vamshi.field.ui.navigation.AdaptiveNavigationWrapper
import com.vamshi.field.ui.theme.AlearningTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AlearningTheme {
                val navController = rememberNavController()
                AdaptiveNavigationWrapper(navController = navController) { modifier ->
                    ALearningNavGraph(
                        navController = navController,
                        modifier = modifier
                    )
                }
            }
        }
    }
}
