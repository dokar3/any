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
    val adjustableMaxSizes: List<Long>? = null,
    private val onClean: () -> Unit,
    private val onUpdateMaxSize: (maxSize: Long) -> Unit,
) {
    fun clean() {
        onClean()
    }

    fun updateMaxSize(maxSize: Long) {
        onUpdateMaxSize(maxSize)
    }
}
