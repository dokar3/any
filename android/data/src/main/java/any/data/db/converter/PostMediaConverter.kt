package any.data.db.converter

import androidx.room.TypeConverter
import any.data.entity.Post
import any.data.json.Json
import any.data.json.fromJson
import any.data.json.toJson

class PostMediaConverter {
    @TypeConverter
    fun toString(media: List<Post.Media>): String {
        return Json.toJson(media)
    }

    @TypeConverter
    fun fromString(text: String): List<Post.Media>? {
        return Json.fromJson<List<Post.Media>>(text)
    }
}