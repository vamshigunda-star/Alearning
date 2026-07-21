package com.vamshi.field.ui.components.video

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vamshi.field.ui.components.TestThumbnail

/**
 * The single video entry point shared by Test Library and Recommendations: renders the
 * [TestThumbnail] slot (placeholder when [youtubeId] is null) and owns the show/hide state
 * of the [VideoPlayerModal] so callers never wire playback themselves.
 */
@Composable
fun TestVideoPreview(
    youtubeId: String?,
    testName: String,
    modifier: Modifier = Modifier,
    height: Dp = 135.dp,
    cornerShape: RoundedCornerShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
) {
    var showVideoModal by remember { mutableStateOf(false) }

    TestThumbnail(
        youtubeId = youtubeId,
        testName = testName,
        onPlayClick = { showVideoModal = true },
        modifier = modifier,
        height = height,
        cornerShape = cornerShape,
    )

    if (showVideoModal && youtubeId != null) {
        VideoPlayerModal(
            youtubeId = youtubeId,
            onDismiss = { showVideoModal = false },
            videoTitle = testName,
        )
    }
}
