package any.data.db.converter

import androidx.room.TypeConverter
import any.data.entity.ServiceResource
import any.data.json.Json
import any.data.json.fromJson
import any.data.json.toJson

class ServiceResourcesConverter {
    @TypeConverter
    fun toString(resources: List<ServiceResource>): String {
        return Json.toJson(resources)
    }

    @TypeConverter
    fun fromString(string: String): List<ServiceResource>? {
        return try {
            Json.fromJson<List<ServiceResource>>(string)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}