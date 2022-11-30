package any.ui.post.header

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import any.data.entity.Post
import any.domain.entity.UiPost
import any.domain.entity.UiServiceManifest
import any.ui.post.viewmodel.PostUiState

@Composable
internal fun Header(
    post: UiPost,
    service: UiServiceManifest?,
    uiState: PostUiState,
    onContinueReadingClick: () -> Unit,
    onSearchTextRequest: (String) -> Unit,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (post.type) {
        Post.Type.Comic -> {
            ComicHeader(
                post = post,
                service = service,
                uiState = uiState,
                onContinueReadingClick = onContinueReadingClick,
                onSearchTextRequest = onSearchTextRequest,
                onUserClick = onUserClick,
                modifier = modifier,
            )
        }
        else -> {
            ArticleHeader(
                post = post,
                service = service,
                onSearchTextRequest = onSearchTextRequest,
                onUserClick = onUserClick,
                modifier = modifier,
            )
        }
    }
}
