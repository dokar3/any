package any.data.db.converter

import androidx.room.TypeConverter
import any.data.Json
import any.data.entity.JsPageKey

class JsPageKeyConverter {
    @TypeConverter
    fun toString(key: JsPageKey): String {
        return Json.toJson(src = key, clz = JsPageKey::class.java)
    }

    @TypeConverter
    fun fromString(value: String): JsPageKey? {
        return Json.fromJson(json = value, clz = JsPageKey::class.java)
    }
}