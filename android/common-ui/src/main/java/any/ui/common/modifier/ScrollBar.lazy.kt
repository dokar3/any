package any.ui.common.modifier

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastSumBy
import any.base.compose.rememberProvider
import any.ui.common.lazy.LazyGridScrollableState
import any.ui.common.lazy.LazyListScrollableState
import any.ui.common.lazy.LazyScrollableState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs

fun Modifier.verticalScrollBar(
    state: LazyListState,
    color: Color = Color.Unspecified,
    width: Dp = 4.dp,
    padding: PaddingValues = PaddingValues(0.dp),
    autoFadeOutDelay: Long = 300L,
): Modifier = composed {
    val provider = rememberProvider {
        state
    }.also {
        it.provide(state)
    }
    verticalScrollBar(
        state = remember { LazyListScrollableState(provider) },
        color = color,
        width = width,
        padding = padding,
        autoFadeOutDelay = autoFadeOutDelay,
    )
}

fun Modifier.verticalScrollBar(
    state: LazyGridState,
    color: Color = Color.Unspecified,
    width: Dp = 4.dp,
    padding: PaddingValues = PaddingValues(0.dp),
    autoFadeOutDelay: Long = 300L,
): Modifier = composed {
    val provider = rememberProvider {
        state
    }.also {
        it.provide(state)
    }
    verticalScrollBar(
        state = remember { LazyGridScrollableState(provider) },
        color = color,
        width = width,
        padding = padding,
        autoFadeOutDelay = autoFadeOutDelay,
    )
}

fun Modifier.verticalScrollBar(
    state: LazyScrollableState,
    color: Color = Color.Unspecified,
    width: Dp = 4.dp,
    padding: PaddingValues = PaddingValues(0.dp),
    autoFadeOutDelay: Long = 300L,
): Modifier = composed {
    val scope = rememberCoroutineScope()

    var scrollBarOffset by remember { mutableStateOf(0f) }

    var scrollBarHeight by remember { mutableStateOf(0) }

    val scrollBarColor = if (color == Color.Unspecified) {
        MaterialTheme.colors.onBackground.copy(alpha = 0.3f)
    } else {
        color
    }

    val alpha = remember { Animatable(1f) }

    val density = LocalDensity.current

    val (topPadding, bottomPadding) = remember(padding) {
        padding.calculateTopPadding() to padding.calculateBottomPadding()
    }

    LaunchedEffect(state, density, topPadding, bottomPadding) {
        val topPaddingPx = with(density) { topPadding.toPx() }
        val bottomPaddingPx = with(density) { bottomPadding.toPx() }
        snapshotFlow { state.visibleItemsInfo }
            .map { it.firstOrNull() }
            .distinctUntilChanged()
            .collect { firstVisibleItem ->
                if (firstVisibleItem == null) {
                    // No items, hide scroll bar
                    scrollBarHeight = 0
                    return@collect
                }

                val visibleItemsInfo = state.visibleItemsInfo

                val itemCount = state.totalItemsCount
                val visibleItemCount = visibleItemsInfo.size

                val viewportHeight = state.viewportSize.height -
                        topPaddingPx -
                        bottomPaddingPx
                val visibleItemsHeight = visibleItemsInfo.fastSumBy { it.size }.toFloat()

                if (visibleItemsHeight <= state.viewportSize.height &&
                    firstVisibleItem.index == 0 &&
                    visibleItemsInfo.last().index == itemCount - 1
                ) {
                    // List may not scrollable, hide scroll bar
                    scrollBarHeight = 0
                    return@collect
                }

                // Assume every item has same height
                val pxPerItem = viewportHeight / itemCount

                // Item count which is in the list viewport
                val inViewportItemCount = viewportHeight / visibleItemsHeight * visibleItemCount

                val columnCount = state.columnCount

                scrollBarHeight = (inViewportItemCount * pxPerItem * columnCount).toInt()

                // Use first item scroll offset to calculate additional offset will make scrolling
                // of scroll bar more smoother
                val averageItemHeight = visibleItemsHeight / visibleItemCount
                val dy = abs(firstVisibleItem.offset).toFloat() / averageItemHeight *
                        pxPerItem *
                        columnCount
                scrollBarOffset = topPaddingPx + firstVisibleItem.index * pxPerItem + dy
            }
    }

    LaunchedEffect(state, autoFadeOutDelay) {
        var fadeIn: Job? = null
        var fadeOut: Job? = null
        snapshotFlow { state.isScrollInProgress }
            .distinctUntilChanged()
            .collect { isScrollInProgress ->
                if (isScrollInProgress) {
                    fadeOut?.cancel()
                    if (fadeIn?.isActive != true) {
                        fadeIn = scope.launch { alpha.animateTo(1f) }
                    }
                } else {
                    fadeOut?.cancel()
                    fadeOut = scope.launch {
                        delay(autoFadeOutDelay)
                        // Fade out
                        alpha.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(durationMillis = 500)
                        )
                    }
                }
            }
    }

    drawWithContent {
        drawContent()

        val scrollBarWidth = width.toPx()
        if (alpha.value > 0f && scrollBarHeight > 0) {
            drawRect(
                color = scrollBarColor,
                topLeft = Offset(size.width - scrollBarWidth, scrollBarOffset),
                size = Size(scrollBarWidth, scrollBarHeight.toFloat()),
                alpha = alpha.value,
            )
        }
    }
}
