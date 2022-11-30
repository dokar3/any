package any.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import any.data.entity.User

@Dao
interface UserDao {
    @Query("select count(*) from User")
    suspend fun count(): Int

    @Query("select * from User where serviceId = :serviceId and id = :id")
    suspend fun get(serviceId: String, id: String): User?

    @Query("select * from User")
    suspend fun getAll(): List<User>

    @Query("select * from User where followedAt >= 0 order by followedAt desc")
    suspend fun getFollowing(): List<User>

    @Query("select * from User where serviceId = :serviceId and followedAt >= 0 order by followedAt desc")
    suspend fun getFollowing(serviceId: String): List<User>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(users: List<User>)

    @Update
    suspend fun update(user: User)

    @Update
    suspend fun update(users: List<User>)

    @Delete
    suspend fun remove(user: User)

    @Query("delete from User")
    suspend fun clear()
}