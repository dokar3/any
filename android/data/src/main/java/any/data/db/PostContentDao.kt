package any.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import any.data.entity.PostContent

@Dao
interface PostContentDao {
    @Query("select * from PostContent where url = :url")
    suspend fun get(url: String): PostContent?

    @Query("select * from PostContent")
    suspend fun getAll(): List<PostContent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(content: PostContent)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(contents: List<PostContent>)

    @Delete
    suspend fun remove(content: PostContent)

    @Query("delete from PostContent where url = :url")
    suspend fun remove(url: String)

    @Update
    suspend fun update(content: PostContent)

    @Query("select url from PostContent")
    suspend fun keys(): List<String>

    @Query("select count(*) from PostContent")
    suspend fun count(): Int

    @Query("delete from PostContent")
    suspend fun clear()
}