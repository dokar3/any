package any.ui.common.dialog

import androidx.compose.runtime.Stable
import any.base.model.PostFolderSelectionSorting
import any.data.entity.HierarchicalFolder

@Stable
data class PostFolderSelectionUiState(
    val error: String? = null,
    val flattedFolders: List<HierarchicalFolder> = emptyList(),
    val selectedFolder: HierarchicalFolder = HierarchicalFolder.ROOT,
    val folderSorting: PostFolderSelectionSorting = PostFolderSelectionSorting.ByTitle,
)