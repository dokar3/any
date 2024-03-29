package any.ui.home.collections

import androidx.compose.runtime.Stable
import androidx.compose.ui.text.input.TextFieldValue
import any.base.model.FolderViewType
import any.base.model.PostSorting
import any.base.model.UiMessage
import any.data.entity.Folder
import any.domain.entity.UiPost

@Stable
data class CollectionsUiState(
    val isLoading: Boolean = false,
    val error: UiMessage.Error? = null,
    val selectedPosts: Set<UiPost> = emptySet(),
    val filterText: TextFieldValue = TextFieldValue(),
    val sorting: PostSorting = PostSorting.ByAddTime,
    val currentFolderUiState: FolderUiState = FolderUiState(),
    val previousFolderUiState: FolderUiState = FolderUiState(),
) {
    fun isSelectionEnabled() = selectedPosts.isNotEmpty()
}

@Stable
data class FolderUiState(
    val folder: Folder = Folder.ROOT,
    val viewType: FolderViewType = FolderViewType.Grid,
    val isLoading: Boolean = false,
    val tags: List<SelectableTag> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val posts: List<FolderPost> = emptyList(),
)