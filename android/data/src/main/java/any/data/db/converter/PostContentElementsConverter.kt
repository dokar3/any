package any.data.db.converter

import androidx.room.TypeConverter
import any.data.entity.ContentElement
import any.data.json.Json
import any.data.json.fromJson
import any.data.json.toJson
import com.squareup.moshi.JsonDataException

class PostContentElementsConverter {
    @TypeConverter
    fun toString(elements: List<ContentElement>): String {
        return Json.toJson(elements)
    }

    @TypeConverter
    fun fromString(text: String): List<ContentElement>? {
        return try {
            Json.fromJson<List<ContentElement>>(text)
        } catch(e: JsonDataException) {
            e.printStackTrace()
            emptyList()
        }
    }
}