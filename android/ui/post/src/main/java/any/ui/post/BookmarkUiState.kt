package any.ui.post

import androidx.compose.runtime.Stable
import any.data.entity.Bookmark

@Stable
data class BookmarkUiState(
    val bookmarks: List<Bookmark> = emptyList(),
)