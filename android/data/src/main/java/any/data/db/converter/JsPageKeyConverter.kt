package any.data.db.converter

import androidx.room.TypeConverter
import any.data.entity.JsPageKey
import any.data.json.Json
import any.data.json.fromJson
import any.data.json.toJson

class JsPageKeyConverter {
    @TypeConverter
    fun toString(key: JsPageKey): String {
        return Json.toJson(key)
    }

    @TypeConverter
    fun fromString(value: String): JsPageKey? {
        return Json.fromJson(value)
    }
}