package any.data.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey
import any.base.prefs.FolderViewType

@Immutable
@Entity
data class FolderInfo(
    @PrimaryKey
    val path: String,
    val viewType: FolderViewType = FolderViewType.Grid,
    val createdAt: Long = System.currentTimeMillis(),
)