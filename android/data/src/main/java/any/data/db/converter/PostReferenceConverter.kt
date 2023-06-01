package any.data.db.converter

import androidx.room.TypeConverter
import any.data.entity.Post
import any.data.json.Json
import any.data.json.fromJson
import any.data.json.toJson

class PostReferenceConverter {
    @TypeConverter
    fun toString(media: Post.Reference): String {
        return Json.toJson(media)
    }

    @TypeConverter
    fun fromString(text: String): Post.Reference? {
        return runCatching {
            Json.fromJson<Post.Reference>(text)
        }.getOrNull()
    }
}