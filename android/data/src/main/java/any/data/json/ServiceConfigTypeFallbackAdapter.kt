package any.data.json

import any.data.entity.ServiceConfigType
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

class ServiceConfigTypeFallbackAdapter {
    @ToJson
    fun toJson(
        writer: JsonWriter,
        value: ServiceConfigType,
        delegate: JsonAdapter<ServiceConfigType>
    ) {
        delegate.toJson(writer, value)
    }

    @FromJson
    fun fromJson(
        reader: JsonReader,
        delegate: JsonAdapter<ServiceConfigType>
    ): ServiceConfigType {
        return try {
            delegate.fromJson(reader) ?: ServiceConfigType.Text
        } catch (e: JsonDataException) {
            e.printStackTrace()
            ServiceConfigType.Text
        }
    }
}