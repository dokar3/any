package any.ui.home.fresh

import androidx.compose.runtime.Immutable
import any.base.image.ImageRequest
import any.base.model.UiMessage
import any.data.entity.JsPageKey
import any.domain.entity.UiPost
import any.domain.entity.UiServiceManifest

@Immutable
data class FreshUiState(
    val services: List<UiServiceManifest> = emptyList(),
    val currService: UiServiceManifest? = null,
    val isLoadingInitialPosts: Boolean = true,
    val isLoadingMorePosts: Boolean = false,
    val isSuccess: Boolean = true,
    val hasMore: Boolean = true,
    val pageKey: JsPageKey? = null,
    val posts: List<UiPost> = emptyList(),
    val allPostMediaImages: List<ImageRequest> = emptyList(),
    val message: UiMessage? = null,
    val requireRefreshInitialPage: Boolean = false,
) {
    fun isLoading(): Boolean = isLoadingInitialPosts || isLoadingMorePosts
}
