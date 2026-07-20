package com.example.alearning.ui.theme

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext

val TorqueEasing = CubicBezierEasing(0.6f, -0.28f, 0.735f, 0.045f)

fun triggerAcceleratorHaptic(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            val vibrator = vibratorManager.defaultVibrator
            val effect = VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_LOW_TICK, 0.3f)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_TICK, 0.6f, 30)
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 60)
                .compose()
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 30, 30, 40, 30, 50)
                val amplitudes = intArrayOf(0, 50, 0, 150, 0, 255)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(150)
            }
        }
    } catch (_: SecurityException) {
        // VIBRATE permission not granted — degrade gracefully
    } catch (_: Exception) {
        // Unexpected vibration error — never crash over haptics
    }
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.acceleratorClick(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "AcceleratorScale"
    )

    this
        .scale(scale)
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                triggerAcceleratorHaptic(context)
                onClick()
            },
            onLongClick = onLongClick?.let {
                {
                    triggerAcceleratorHaptic(context)
                    it()
                }
            }
        )
}

@Composable
fun TorqueTransitionWrapper(
    content: @Composable () -> Unit
) {
    var isEntering by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        isEntering = false
    }

    val blurRadius by animateFloatAsState(
        targetValue = if (isEntering) 40f else 0f,
        animationSpec = tween(durationMillis = 350, easing = TorqueEasing),
        label = "MotionBlur"
    )

    androidx.compose.foundation.layout.Box(
        modifier = Modifier.graphicsLayer {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadius > 0.1f) {
                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                    blurRadius,
                    1f, 
                    android.graphics.Shader.TileMode.CLAMP
                ).asComposeRenderEffect()
            }
        }
    ) {
        content()
    }
}
