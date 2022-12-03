package any.ui.common.lazy

import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntSize
import any.base.compose.Provider
import any.base.compose.rememberSaveableProvider
import any.ui.common.quickScrollToTop

@Composable
fun rememberLazyListScrollableState(
    listStateProvider: Provider<LazyListState> = rememberSaveableProvider(
        inputs = emptyArray(),
        saver = LazyListState.Saver,
    ) {
        LazyListState()
    },
): LazyListScrollableState {
    return remember {
        LazyListScrollableState(listStateProvider)
    }
}

private fun LazyListItemInfo.toLazyItemInfo(): LazyItemInfo {
    return LazyItemInfo(
        index = index,
        offset = offset,
        size = size,
        key = key,
    )
}

@Stable
class LazyListScrollableState(
    private val listStateProvider: Provider<LazyListState>
) : LazyScrollableState {
    val listState: LazyListState
        get() = listStateProvider.get()

    override val visibleItemsInfo: List<LazyItemInfo>
        get() = listState.layoutInfo.visibleItemsInfo.map { it.toLazyItemInfo() }

    override val totalItemsCount: Int
        get() = listState.layoutInfo.totalItemsCount

    override val viewportSize: IntSize
        get() = listState.layoutInfo.viewportSize

    override val viewportStartOffset: Int
        get() = listState.layoutInfo.viewportStartOffset

    override val viewportEndOffset: Int
        get() = listState.layoutInfo.viewportEndOffset

    override val columnCount: Float = 1f

    override val beforeContentPadding: Int
        get() = listState.layoutInfo.beforeContentPadding

    override val afterContentPadding: Int
        get() = listState.layoutInfo.afterContentPadding

    override val isScrollInProgress: Boolean
        get() = listState.isScrollInProgress

    override suspend fun quickScrollToTop() {
        listState.quickScrollToTop()
    }
}
