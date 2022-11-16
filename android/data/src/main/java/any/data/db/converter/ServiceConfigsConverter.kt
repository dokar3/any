package any.data.db.converter

import androidx.room.TypeConverter
import any.data.Json
import any.data.entity.ServiceConfig

class ServiceConfigsConverter {
    @TypeConverter
    fun toString(fields: List<ServiceConfig>): String {
        return Json.toJson(fields, List::class.java)
    }

    @TypeConverter
    fun fromString(text: String): List<ServiceConfig>? {
        val type = Json.parameterizedType<List<ServiceConfig>>()
        return Json.fromJson(text, type)
    }
}
