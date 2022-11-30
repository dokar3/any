package any.ui.profile.viewmodel

import androidx.compose.runtime.Immutable
import any.base.UiMessage
import any.domain.entity.UiPost
import any.domain.entity.UiServiceManifest
import any.domain.entity.UiUser

@Immutable
data class ProfileUiState(
    val service: UiServiceManifest? = null,
    val user: UiUser? = null,
    val posts: List<UiPost> = emptyList(),
    val isLoadingUser: Boolean = false,
    val isLoadingPosts: Boolean = false,
    val isLoadingMorePosts: Boolean = false,
    val isFailedToFetchPosts: Boolean = false,
    val hasMore: Boolean = true,
    val message: UiMessage? = null,
)
