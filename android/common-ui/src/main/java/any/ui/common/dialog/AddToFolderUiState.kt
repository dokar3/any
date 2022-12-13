package any.ui.common.dialog

import androidx.compose.runtime.Stable
import any.data.entity.HierarchicalFolder

@Stable
data class AddToFolderUiState(
    val flattedFolders: List<HierarchicalFolder> = emptyList(),
    val selectedFolder: HierarchicalFolder = HierarchicalFolder.ROOT,
    val folderSorting: AddToFolderSorting = AddToFolderSorting.ByTitle,
)

enum class AddToFolderSorting {
    ByTitle,
    ByLastUpdatedTime,
}