package com.example.alearning.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Standard top bar for every screen in ALearning.
 *
 * Visual contract (do not override per-screen):
 *  - CenterAlignedTopAppBar
 *  - Title: typography.titleLarge (22sp Bold)
 *  - Container: colorScheme.primary (NavyPrimary)
 *  - Title / icons: white
 *
 * Use the [navigationIcon] slot for a back button on detail screens.
 * Use the [actions] slot for trailing action buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    AppTopBar(
        modifier = modifier,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = navigationIcon,
        actions = actions,
    )
}

/**
 * Composable-title overload for screens that need a multi-line title
 * (e.g. screen heading + subtitle like a date or group name).
 *
 * The visual contract still applies — wrap your title content in the
 * navy container with white text. Use [AppTopBarSubtitleStyle] /
 * [AppTopBarSubtitleColor] for subtitle text below the main title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White,
            actionIconContentColor = Color.White,
        ),
        modifier = modifier,
    )
}

/** Subtitle text color to use beneath the main title when content slot is used. */
val AppTopBarSubtitleColor: Color
    get() = Color.White.copy(alpha = 0.7f)
