package any.ui.post.viewmodel

import androidx.compose.runtime.Stable
import any.data.entity.Bookmark
import any.domain.entity.UiContentElement
import any.domain.entity.UiPost
import any.domain.entity.UiServiceManifest
import any.domain.post.ContentSection

@Stable
data class PostUiState(
    val service: UiServiceManifest? = null,
    val isLoading: Boolean = false,
    val loadingProgress: LoadingProgress? = null,
    val reversedPages: Boolean = false,
    val post: UiPost? = null,
    val error: Throwable? = null,
    val isCollected: Boolean = false,
    val contentElements: List<UiContentElement> = emptyList(),
    val images: List<String> = emptyList(),
    val sections: List<ContentSection> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
) {
    val hasComments = post?.commentsKey != null

    val sectionCount = sections.count { !it.isStart && !it.isEnd }
}

data class LoadingProgress(
    val value: Float = 0f,
    val message: String? = null,
)