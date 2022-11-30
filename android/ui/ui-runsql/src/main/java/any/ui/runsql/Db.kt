package any.ui.runsql

import androidx.compose.runtime.Immutable
import androidx.room.RoomDatabase

@Immutable
internal data class Db(
    val name: String,
    private val creator: () -> RoomDatabase,
) {
    fun create(): RoomDatabase = creator()
}