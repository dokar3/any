package any.data.json

import any.data.entity.ServiceConfigValue
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

class ServiceConfigValueAdapter {
    @ToJson
    fun toJson(value: ServiceConfigValue): String {
        return value.text ?: ""
    }

    @FromJson
    fun fromJson(value: Any): ServiceConfigValue {
        return ServiceConfigValue(value)
    }
}