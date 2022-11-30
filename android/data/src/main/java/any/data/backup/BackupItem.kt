package any.data.backup

import any.data.entity.AppDataType
import java.io.File

data class BackupItem(
    val type: AppDataType,
    val file: File,
)