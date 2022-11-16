package any.data.db.converter

import androidx.room.TypeConverter
import any.data.Json
import any.data.entity.Post

class PostMediaConverter {
    @TypeConverter
    fun toString(media: List<Post.Media>): String {
        return Json.toJson(src = media, clz = List::class.java)
    }

    @TypeConverter
    fun fromString(text: String): List<Post.Media>? {
        return Json.fromJson(
            json = text,
            type = Json.parameterizedType<List<Post.Media>>()
        )
    }
}