package any.data.db.converter

import androidx.room.TypeConverter
import any.data.entity.JsPageKey
import any.data.json.Json

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