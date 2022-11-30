package any.data.db.converter

import androidx.room.TypeConverter
import any.data.entity.ContentElement
import any.data.json.Json
import com.squareup.moshi.JsonDataException

class PostContentElementsConverter {
    @TypeConverter
    fun toString(elements: List<ContentElement>): String {
        return Json.toJson(elements, List::class.java)
    }

    @TypeConverter
    fun fromString(text: String): List<ContentElement>? {
        val type = Json.parameterizedType<List<ContentElement>>()
        return try {
            Json.fromJson(text, type)
        } catch(e: JsonDataException) {
            e.printStackTrace()
            emptyList()
        }
    }
}