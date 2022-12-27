package any.data.json

import any.data.entity.ServiceConfig
import any.data.entity.ServiceConfigType
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.ToJson

class ServiceConfigAdapter {
    @FromJson
    fun fromJson(source: Map<*, *>): ServiceConfig? {
        val rawType = source["type"]
            ?: throw JsonDataException("Missing the 'type' field")
        if (rawType !is String) {
            throw JsonDataException("'type' field only accepts a string")
        }
        val type = ServiceConfigType.fromValueOrNull(rawType)
            ?: throw JsonDataException("Unknown config type '${rawType}'")
        val json = Json.toJson(source, Map::class.java)
        return when (type) {
            ServiceConfigType.Bool -> Json.fromJson(json, ServiceConfig.Bool::class.java)
            ServiceConfigType.Number -> Json.fromJson(json, ServiceConfig.Number::class.java)
            ServiceConfigType.Text -> Json.fromJson(json, ServiceConfig.Text::class.java)
            ServiceConfigType.Url -> Json.fromJson(json, ServiceConfig.Url::class.java)
            ServiceConfigType.Option -> Json.fromJson(json, ServiceConfig.Option::class.java)
            ServiceConfigType.Cookies -> Json.fromJson(json, ServiceConfig.Cookies::class.java)
            ServiceConfigType.CookiesUa -> Json.fromJson(json, ServiceConfig.CookiesUa::class.java)
        }
    }

    @ToJson
    fun toJson(value: ServiceConfig): String {
        return when (value) {
            is ServiceConfig.Bool -> Json.toJson(value, ServiceConfig.Bool::class.java)
            is ServiceConfig.Number -> Json.toJson(value, ServiceConfig.Number::class.java)
            is ServiceConfig.Text -> Json.toJson(value, ServiceConfig.Text::class.java)
            is ServiceConfig.Url -> Json.toJson(value, ServiceConfig.Url::class.java)
            is ServiceConfig.Option -> Json.toJson(value, ServiceConfig.Option::class.java)
            is ServiceConfig.Cookies -> Json.toJson(value, ServiceConfig.Cookies::class.java)
            is ServiceConfig.CookiesUa -> Json.toJson(value, ServiceConfig.CookiesUa::class.java)
        }
    }
}