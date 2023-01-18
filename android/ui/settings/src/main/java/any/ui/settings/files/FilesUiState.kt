package any.ui.settings.files

import androidx.compose.runtime.Stable
import any.data.entity.SpaceInfo

@Stable
data class FilesUiState(
    val cleanableItems: List<CleanableItem> = emptyList(),
)

@Stable
data class CleanableItem(
    val id: Int,
    val name: String,
    val cleanDescription: String,
    val spaceInfo: SpaceInfo,
    private val onClean: () -> Unit,
) {
    fun clean() {
        onClean()
    }
}
