package any.data.db.converter

import androidx.room.TypeConverter
import any.data.entity.Checksums
import any.data.json.Json
import any.data.json.fromJson
import any.data.json.toJson

class ChecksumsConverter {
    @TypeConverter
    fun toString(checksums: Checksums): String {
        return Json.toJson(checksums)
    }

    @TypeConverter
    fun fromString(value: String): Checksums? {
        return try {
            Json.fromJson(value)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}