package any.ui.settings.files

import androidx.compose.runtime.Stable
import any.data.entity.AppDataType
import java.io.File

@Stable
data class BackupUiState(
    val items: List<BackupUiItem> = emptyList(),
    val isExporting: Boolean = false,
    val isExported: Boolean = false,
    val isLoadingBackup: Boolean = false,
    val isImporting: Boolean = false,
    val isImported: Boolean = false,
)

@Stable
data class BackupUiItem(
    val id: Int,
    val typeName: String,
    val type: AppDataType,
    val select: () -> Unit,
    val unselect: () -> Unit,
    val file: File? = null,
    val count: Int = 0,
    val successCount: Int = 0,
    val isSelected: Boolean = true,
)