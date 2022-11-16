package any.data.db.converter

import androidx.room.TypeConverter
import any.data.Json
import any.data.entity.Checksums

class ChecksumsConverter {
    @TypeConverter
    fun toString(checksums: Checksums): String {
        return Json.toJson(checksums, Checksums::class.java)
    }

    @TypeConverter
    fun fromString(value: String): Checksums? {
        return try {
            Json.fromJson(value, Checksums::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}