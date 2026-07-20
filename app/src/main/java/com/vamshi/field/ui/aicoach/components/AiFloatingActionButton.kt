package com.example.alearning.ui.aicoach.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable

@Composable
fun AiFloatingActionButton(
    isVisible: Boolean,
    onClick: () -> Unit
) {
    if (isVisible) {
        FloatingActionButton(onClick = onClick) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Coach")
        }
    }
}
