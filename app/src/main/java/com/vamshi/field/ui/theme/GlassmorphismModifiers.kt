package com.vamshi.field.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glassmorphismFallback(
    radius: Dp = 16.dp,
    fallbackColor: Color = Color(0xB3F2F2F7) // 70% opacity off-white
): Modifier = composed {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Native blur available on API 31+
        this.blur(radius = radius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
            .background(Color.White.copy(alpha = 0.5f))
    } else {
        // Fallback for older devices
        this.background(fallbackColor)
    }
}
