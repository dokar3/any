package any.ui.common

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState

val LazyListState.itemRange: IntRange
    get() = 0..layoutInfo.totalItemsCount

val LazyListState.visibleItemRange: IntRange
    get() {
        val items = layoutInfo.visibleItemsInfo
        if (items.isEmpty()) {
            return IntRange.EMPTY
        }
        return items.first().index..items.last().index
    }

/**
 * Useful function from: [stackoverflow](https://stackoverflow.com/a/71720929)
 */
fun LazyListState.isLastItemVisible(): Boolean {
    return layoutInfo.visibleItemsInfo.lastOrNull()?.index == layoutInfo.totalItemsCount - 1
}

/**
 * Based on: https://stackoverflow.com/a/69547214
 */
fun LazyListState.isScrolledToTheEnd(bottomPadding: Int = 0): Boolean {
    val lastItem = layoutInfo.visibleItemsInfo.lastOrNull() ?: return true
    return lastItem.size + lastItem.offset + bottomPadding <= layoutInfo.viewportEndOffset
}

suspend fun LazyListState.quickScrollToTop() {
    if (firstVisibleItemIndex > 9) {
        scrollToItem(9)
    }
    animateScrollToItem(0)
}

suspend fun LazyGridState.quickScrollToTop() {
    if (firstVisibleItemIndex > 9) {
        scrollToItem(9)
    }
    animateScrollToItem(0)
}

suspend fun LazyListState.quickScrollToEnd() {
    val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return
    val lastItem = layoutInfo.totalItemsCount - 1
    if (lastItem - lastVisibleIndex > 9) {
        scrollToItem(lastItem - 10)
    }
    animateScrollToItem(lastItem)
}
