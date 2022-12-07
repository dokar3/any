package any.data.json

import any.data.entity.ServiceConfigValue
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson

class ServiceConfigValueAdapter {
    @ToJson
    fun toJson(value: ServiceConfigValue): String {
        return value.stringValue
    }

    @FromJson
    fun fromJson(value: Any): ServiceConfigValue {
        return when (value) {
            is Boolean -> ServiceConfigValue.Boolean(value)

            is Double -> ServiceConfigValue.Double(value)

            is String -> try {
                Json.fromJson(value, ServiceConfigValue.CookiesAndUa::class.java)!!
            } catch (e: Exception) {
                ServiceConfigValue.String(value)
            }

            else -> {
                throw JsonDataException(
                    "Unknown config value: $value, class: ${value::class.java}"
                )
            }
        }
    }
}