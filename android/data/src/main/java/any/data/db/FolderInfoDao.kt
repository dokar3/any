package any.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import any.data.entity.FolderInfo

@Dao
interface FolderInfoDao {
    @Query("select * from FolderInfo where path = :path")
    suspend fun get(path: String): FolderInfo?

    @Query("select * from FolderInfo")
    suspend fun getAll(): List<FolderInfo>

    @Insert
    suspend fun add(folderInfo: FolderInfo)

    @Insert
    suspend fun add(list: List<FolderInfo>)

    @Update
    suspend fun update(folderInfo: FolderInfo)

    @Delete
    suspend fun remove(folderInfo: FolderInfo)

    @Query("delete from FolderInfo where path = :path")
    suspend fun remove(path: String)

    @Query("delete from FolderInfo")
    suspend fun clear()
}