package any.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import any.data.entity.Post

@Dao
interface PostDao {
    @Query("select count(*) from Post")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(post: Post)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(posts: List<Post>)

    @Query("select * from Post where url = :url and serviceId = :serviceId")
    suspend fun get(serviceId: String, url: String): Post?

    @Query("select * from Post where serviceId = :serviceId")
    suspend fun getByServiceId(serviceId: String): List<Post>

    @Query("select * from Post order by createdAt desc")
    suspend fun getAll(): List<Post>

    @Query("select * from Post where serviceId = :serviceId and orderInFresh >= 0 order by orderInFresh")
    suspend fun getFresh(serviceId: String): List<Post>

    @Query("select * from Post where serviceId = :serviceId and authorId = :userId")
    suspend fun getUserPosts(serviceId: String, userId: String): List<Post>

    @Query("select * from Post where serviceId = :serviceId and authorId = :userId and orderInProfile >= 0 order by orderInProfile")
    suspend fun getInProfile(serviceId: String, userId: String): List<Post>

    @Query("select * from Post where collectedAt > 0 order by collectedAt desc")
    suspend fun getCollected(): List<Post>

    @Query("select * from Post where downloadAt > 0 order by downloadAt desc")
    suspend fun getInDownload(): List<Post>

    @Update
    suspend fun update(post: Post)

    @Update
    suspend fun update(posts: List<Post>)

    @Delete
    suspend fun remove(post: Post)

    @Delete
    suspend fun remove(posts: List<Post>)

    @Query("delete from Post where serviceId = :serviceId and url = :url")
    suspend fun remove(serviceId: String, url: String)

    @Query("delete from Post where serviceId = :serviceId and orderInFresh >= 0 and collectedAt < 0 and downloadAt < 0")
    suspend fun clearFresh(serviceId: String)

    @Query("delete from Post where serviceId = :serviceId and orderInFresh < 0 and orderInProfile < 0 and collectedAt < 0 and downloadAt < 0")
    suspend fun clearUnused(serviceId: String)

    @Query("delete from Post")
    suspend fun clear()
}