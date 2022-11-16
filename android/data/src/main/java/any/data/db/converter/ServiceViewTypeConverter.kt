package any.data.db.converter

import androidx.room.TypeConverter
import any.data.entity.ServiceViewType

class ServiceViewTypeConverter {
    @TypeConverter
    fun toString(viewType: ServiceViewType): String {
        return viewType.name
    }

    @TypeConverter
    fun fromString(value: String): ServiceViewType {
        return try {
            ServiceViewType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ServiceViewType.List
        }
    }
}