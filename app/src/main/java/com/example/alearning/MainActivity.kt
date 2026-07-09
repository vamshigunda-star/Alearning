package com.example.alearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.alearning.ui.navigation.ALearningNavGraph
import com.example.alearning.ui.navigation.AdaptiveNavigationWrapper
import com.example.alearning.ui.theme.AlearningTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
