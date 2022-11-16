package any.ui.common.modifier

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.packFloats

@Composable
fun rememberLazyGridColumnCount(
    state: LazyGridState,
    cells: GridCells,
    spacing: Dp,
): Int {
    val density = LocalDensity.current
    val width by remember {
        derivedStateOf {
            state.layoutInfo.viewportSize.width
        }
    }
    return remember(density, cells, width, spacing) {
        with(cells) {
            density.calculateCrossAxisCellSizes(
                availableSize = width,
                spacing = with(density) { spacing.roundToPx() },
            ).size
        }
    }
}

/**
 * We do not set contentPadding or [Arrangement.spacedBy] to the grid because we may
 * have a full-width header, so we need calculate and set paddings for items manually.
 */
fun Modifier.gridItemPadding(
    spacing: Dp,
    columnCount: Int,
    index: Int,
): Modifier {
    val itemPaddings = RowItemPaddings.get(spacing, columnCount)
    return padding(
        start = itemPaddings.start(index),
        top = spacing,
        end = itemPaddings.end(index),
    )
}

private class RowItemPaddings private constructor(
    spacing: Dp,
    private val columnCount: Int,
) {
    private val startPaddings = Array(columnCount) { 0.dp }
    private val endPaddings = Array(columnCount) { 0.dp }

    init {
        require(spacing >= 0.dp) { "Negative spacings are not allowed" }
        require(columnCount >= 0) { "columnCount cannot be negative" }
        val gaps = columnCount + 1
        // Make sure all items have the same horizontal padding, so they
        // will have the same width.
        val hPaddingPerItem = spacing * gaps / columnCount
        startPaddings[0] = spacing
        endPaddings[0] = hPaddingPerItem - spacing
        for (i in 1 until columnCount) {
            val start = spacing - endPaddings[i - 1]
            startPaddings[i] = start
            endPaddings[i] = hPaddingPerItem - start
        }
    }

    fun start(index: Int): Dp {
        require(index >= 0) { "index cannot be negative" }
        if (columnCount == 0) return Dp.Unspecified
        val indexInRow = index % columnCount
        return startPaddings[indexInRow]
    }

    fun end(index: Int): Dp {
        require(index >= 0) { "index cannot be negative" }
        if (columnCount == 0) return Dp.Unspecified
        val indexInRow = index % columnCount
        return endPaddings[indexInRow]
    }

    companion object {
        private val cache = mutableMapOf<Long, RowItemPaddings>()

        fun get(spacing: Dp, columnCount: Int): RowItemPaddings {
            val key = packFloats(spacing.value, columnCount.toFloat())
            return cache.getOrPut(key) { RowItemPaddings(spacing, columnCount) }
        }
    }
}