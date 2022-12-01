package any.ui.runsql

import androidx.compose.runtime.Stable
import androidx.room.RoomDatabase

@Stable
internal interface Db {
    val name: String

    fun create(): RoomDatabase
}