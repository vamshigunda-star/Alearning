package com.example.alearning.ui.aicoach.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

@Composable
fun DraggableAiFab(
    isVisible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return
    
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var parentSize by remember { mutableStateOf(IntSize.Zero) }
    var fabSize by remember { mutableStateOf(IntSize.Zero) }
    
    // Position it at bottom right initially (approx 16dp margin)
    LaunchedEffect(parentSize, fabSize) {
        if (parentSize != IntSize.Zero && fabSize != IntSize.Zero && offsetX == 0f && offsetY == 0f) {
            offsetX = (parentSize.width - fabSize.width - 48).toFloat() 
            offsetY = (parentSize.height - fabSize.height - 48).toFloat()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { parentSize = it.size }
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .onGloballyPositioned { fabSize = it.size }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newX = (offsetX + dragAmount.x).coerceIn(0f, (parentSize.width - fabSize.width).toFloat().coerceAtLeast(0f))
                        val newY = (offsetY + dragAmount.y).coerceIn(0f, (parentSize.height - fabSize.height).toFloat().coerceAtLeast(0f))
                        offsetX = newX
                        offsetY = newY
                    }
                }
        ) {
            AiFloatingActionButton(isVisible = true, onClick = onClick)
        }
    }
}
