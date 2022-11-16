package any.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import any.data.entity.Bookmark

@Dao
interface BookmarkDao {
    @Query("select count(*) from Bookmark")
    suspend fun count(): Int

    @Query("select * from Bookmark where serviceId = :serviceId and postUrl = :postUrl and elementIndex = :elementIndex")
    suspend fun find(serviceId: String, postUrl: String, elementIndex: Int): Bookmark?

    @Query("select * from Bookmark")
    suspend fun getAll(): List<Bookmark>

    @Query("select * from Bookmark where serviceId = :serviceId and postUrl = :postUrl")
    suspend fun get(serviceId: String, postUrl: String): List<Bookmark>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(bookmark: Bookmark)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(bookmarks: List<Bookmark>)

    @Update
    suspend fun update(bookmark: Bookmark)

    @Delete
    suspend fun remove(bookmark: Bookmark)

    @Query("delete from Bookmark where serviceId = :serviceId and postUrl = :postUrl")
    suspend fun remove(serviceId: String, postUrl: String)

    @Query("delete from Bookmark")
    suspend fun clear()
}