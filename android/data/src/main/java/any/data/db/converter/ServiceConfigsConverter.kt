package any.data.db.converter

import androidx.room.TypeConverter
import any.data.entity.ServiceConfig
import any.data.json.Json
import any.data.json.fromJson
import any.data.json.toJson

class ServiceConfigsConverter {
    @TypeConverter
    fun toString(fields: List<ServiceConfig>): String {
        return Json.toJson(fields)
    }

    @TypeConverter
    fun fromString(text: String): List<ServiceConfig>? {
        return Json.fromJson<List<ServiceConfig>>(text)
    }
}
