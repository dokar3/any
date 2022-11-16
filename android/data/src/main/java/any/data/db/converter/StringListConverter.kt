package any.data.db.converter

import androidx.room.TypeConverter

class StringListConverter {
    @TypeConverter
    fun fromString(text: String): List<String> {
        if (text.isEmpty()) {
            return emptyList()
        }
        return text.split(SEPARATOR)
    }

    @TypeConverter
    fun toString(list: List<String>): String {
        return list.joinToString(SEPARATOR)
    }

    companion object {
        private const val SEPARATOR = "\n"
    }
}
