package any.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import any.data.entity.ServiceManifest

@Dao
interface ServiceDao {
    @Query("select count(*) from Service")
    suspend fun count(): Int

    @Query("select * from Service")
    suspend fun getAll(): List<ServiceManifest>

    @Query("select id from Service")
    suspend fun getAllIds(): List<String>

    @Query("select * from Service where id = :id")
    suspend fun get(id: String): ServiceManifest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(service: ServiceManifest)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(services: List<ServiceManifest>)

    @Update
    suspend fun update(service: ServiceManifest)

    @Update
    suspend fun update(services: List<ServiceManifest>)

    @Delete
    suspend fun remove(service: ServiceManifest)

    @Delete
    suspend fun remove(services: List<ServiceManifest>)

    @Query("delete from Service")
    suspend fun clear()
}