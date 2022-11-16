package any.ui.common.lazy

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntSize

@Stable
interface LazyScrollableState {
    val visibleItemsInfo: List<LazyItemInfo>

    val totalItemsCount: Int

    val viewportSize: IntSize

    val viewportStartOffset: Int

    val viewportEndOffset: Int

    val columnCount: Float

    val beforeContentPadding: Int

    val afterContentPadding: Int

    val isScrollInProgress: Boolean

    suspend fun quickScrollToTop()
}