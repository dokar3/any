package any.data.db.converter

import androidx.room.TypeConverter
import any.data.entity.PostsViewType

class ServiceViewTypeConverter {
    @TypeConverter
    fun toString(viewType: PostsViewType): String {
        return viewType.name
    }

    @TypeConverter
    fun fromString(value: String): PostsViewType {
        return try {
            PostsViewType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            PostsViewType.List
        }
    }
}