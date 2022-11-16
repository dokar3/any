package any.ui.comments.viewmodel

import androidx.compose.runtime.Stable
import any.base.UiMessage
import any.data.entity.JsPageKey

@Stable
data class CommentsUiState(
    val pageKey: JsPageKey? = null,
    val comments: List<UiComment> = emptyList(),
    val hasMore: Boolean = true,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSuccess: Boolean = true,
    val message: UiMessage? = null,
)