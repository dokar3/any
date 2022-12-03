package any.ui.common.lazy

import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntSize
import any.base.compose.Provider
import any.base.compose.rememberSaveableProvider
import any.ui.common.quickScrollToTop

@Composable
fun rememberLazyGridScrollableState(
    gridStateProvider: Provider<LazyGridState> = rememberSaveableProvider(
        inputs = emptyArray(),
        saver = LazyGridState.Saver,
    ) {
        LazyGridState()
    },
): LazyGridScrollableState {
    return remember {
        LazyGridScrollableState(gridStateProvider)
    }
}

private fun LazyGridItemInfo.toLazyItemInfo(): LazyItemInfo {
    return LazyItemInfo(
        index = index,
        offset = offset.y,
        size = size.height,
        key = key,
    )
}

@Stable
class LazyGridScrollableState(
    private val gridStateProvider: Provider<LazyGridState>
) : LazyScrollableState {
    val gridState: LazyGridState
        get() = gridStateProvider.get()

    override val visibleItemsInfo: List<LazyItemInfo>
        get() = gridState.layoutInfo.visibleItemsInfo.map { it.toLazyItemInfo() }

    override val totalItemsCount: Int
        get() = gridState.layoutInfo.totalItemsCount

    override val viewportSize: IntSize
        get() = gridState.layoutInfo.viewportSize

    override val viewportStartOffset: Int
        get() = gridState.layoutInfo.viewportStartOffset

    override val viewportEndOffset: Int
        get() = gridState.layoutInfo.viewportEndOffset

    override val columnCount: Float
        get() {
            val visibleItems = gridState.layoutInfo.visibleItemsInfo
            val rows = mutableSetOf<Int>()
            for (item in visibleItems) {
                if (item.size.height > 0) {
                    rows.add(item.row)
                }
            }
            return visibleItems.size.toFloat() / rows.size
        }

    override val beforeContentPadding: Int
        get() = gridState.layoutInfo.beforeContentPadding

    override val afterContentPadding: Int
        get() = gridState.layoutInfo.afterContentPadding

    override val isScrollInProgress: Boolean
        get() = gridState.isScrollInProgress

    override suspend fun quickScrollToTop() {
        gridState.quickScrollToTop()
    }
}