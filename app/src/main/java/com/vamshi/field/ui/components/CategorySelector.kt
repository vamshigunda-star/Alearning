package com.vamshi.field.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * "Once per app run" gate for the scroll-hint animation. Deliberately an in-memory
 * singleton (not persisted) — the hint is cosmetic and should reappear on a fresh process.
 */
private object CategorySelectorHintState {
    var hasShown = false
}

// AppFilterChip height (38dp) + LazyRow's vertical contentPadding (12dp top + 12dp bottom).
// Fixed explicitly (rather than intrinsic/wrap sizing) because LazyRow doesn't support
// intrinsic measurement, and an unbounded Box height here would let the fillMaxHeight()
// edge-fade overlay balloon to fill the whole remaining Column space.
private val ROW_HEIGHT = 62.dp

@Composable
fun <T> CategorySelector(
    categories: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
) {
    val selectedKey = selected?.let(key)
    val initialIndex = remember { categories.indexOfFirst { key(it) == selectedKey }.coerceAtLeast(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val hintOffset = remember { Animatable(0f) }
    var hasSettledInitialPosition by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!CategorySelectorHintState.hasShown) {
            CategorySelectorHintState.hasShown = true
            hintOffset.animateTo(14f, animationSpec = tween(300))
            hintOffset.animateTo(0f, animationSpec = tween(300))
        }
    }

    LaunchedEffect(selectedKey, categories) {
        val index = categories.indexOfFirst { key(it) == selectedKey }
        if (index < 0) return@LaunchedEffect

        // First settle happens instantly (no animation) so cards land in their final
        // position on appearance instead of visibly sliding in and then correcting.
        // Later selection changes (user tapping a different category) animate normally.
        if (!hasSettledInitialPosition) {
            listState.scrollToItem(index)
            val info = listState.layoutInfo
            val itemInfo = info.visibleItemsInfo.find { it.index == index }
            if (itemInfo != null) {
                val viewportCenter = info.viewportSize.width / 2
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                listState.scrollBy((itemCenter - viewportCenter).toFloat())
            }
            hasSettledInitialPosition = true
        } else {
            listState.animateScrollToItem(index)
            val info = listState.layoutInfo
            val itemInfo = info.visibleItemsInfo.find { it.index == index } ?: return@LaunchedEffect
            val viewportCenter = info.viewportSize.width / 2
            val itemCenter = itemInfo.offset + itemInfo.size / 2
            listState.animateScrollBy((itemCenter - viewportCenter).toFloat())
        }
    }

    val canScrollForward = listState.canScrollForward

    Box(modifier = modifier.fillMaxWidth().height(ROW_HEIGHT)) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .offset(x = hintOffset.value.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(categories, key = { key(it) }) { category ->
                AppFilterChip(
                    label = label(category),
                    isSelected = key(category) == selectedKey,
                    onClick = { onSelect(category) }
                )
            }
        }

        if (canScrollForward) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(32.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, backgroundColor)
                        )
                    )
            )
        }
    }
}
