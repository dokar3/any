package any.data.db.converter

import androidx.room.TypeConverter
import any.data.entity.ServiceResource
import any.data.json.Json

class ServiceResourcesConverter {
    @TypeConverter
    fun toString(resources: List<ServiceResource>): String {
        return Json.toJson(resources, List::class.java)
    }

    @TypeConverter
    fun fromString(string: String): List<ServiceResource>? {
        val type = Json.parameterizedType<List<ServiceResource>>()
        return try {
            Json.fromJson(string, type)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}