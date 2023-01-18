package any.ui.home.collections

import androidx.compose.runtime.Immutable
import any.data.entity.Post
import any.domain.entity.UiPost

internal fun Collection<FolderPost>.containsRaw(post: Post): Boolean {
    return indexOfFirst { it.raw == post } != -1
}

@Immutable
data class FolderPost(
    val ui: UiPost,
    val defaultThumbAspectRatio: Float?,
) {
    val raw: Post get() = ui.raw
}