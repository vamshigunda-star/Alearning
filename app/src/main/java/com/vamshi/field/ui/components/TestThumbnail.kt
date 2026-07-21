package com.vamshi.field.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vamshi.field.ui.util.youtubeThumbnailUrl

/**
 * Shared card-top thumbnail slot for Test Library and Recommendations cards. Always
 * occupies [height] regardless of whether a video exists, so list items stay uniform
 * height — renders a neutral placeholder icon when [youtubeId] is null.
 */
@Composable
fun TestThumbnail(
    youtubeId: String?,
    testName: String,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 135.dp,
    cornerShape: RoundedCornerShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(cornerShape)
            .then(if (youtubeId != null) Modifier.clickable(onClick = onPlayClick) else Modifier)
    ) {
        if (youtubeId != null) {
            AsyncImage(
                model = youtubeThumbnailUrl(youtubeId),
                contentDescription = "Watch $testName demonstration",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Surface(
                modifier = Modifier.size(40.dp).align(Alignment.Center),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier.padding(8.dp).align(Alignment.BottomStart),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            ) {
                Text(
                    "Video Guide",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}
