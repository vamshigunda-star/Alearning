package com.vamshi.field.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.vamshi.field.ui.theme.TextSecondary

/**
 * Standard top bar for every screen in ALearning.
 *
 * Visual contract (do not override per-screen):
 *  - Left-aligned TopAppBar on a white surface with a hairline bottom border
 *  - Title: typography.headlineMedium (28sp SemiBold)
 *  - Container: colorScheme.surface (white); content: colorScheme.onSurface
 *  - Trailing actions use [AppTopBarActionButton] for the rounded,
 *    tinted icon-container treatment instead of a bare IconButton.
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
                style = MaterialTheme.typography.headlineMedium,
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
 * white surface with dark text. Use [AppTopBarSubtitleColor] for subtitle
 * text below the main title.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier = modifier) {
        TopAppBar(
            title = title,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

/** Subtitle text color to use beneath the main title when the content-slot overload is used. */
val AppTopBarSubtitleColor: Color
    get() = TextSecondary

/**
 * Trailing top-bar action styled per the design system: an icon inside a
 * rounded, tinted container rather than a bare [IconButton].
 */
@Composable
fun AppTopBarActionButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(4.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
            ),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
