package any.data.db.converter

import androidx.room.TypeConverter
import any.data.entity.Post
import any.data.json.Json

class PostReferenceConverter {
    @TypeConverter
    fun toString(media: Post.Reference): String {
        return Json.toJson(src = media, clz = Post.Reference::class.java)
    }

    @TypeConverter
    fun fromString(text: String): Post.Reference? {
        return runCatching {
            Json.fromJson(
                json = text,
                clz = Post.Reference::class.java
            )
        }.getOrNull()
    }
}