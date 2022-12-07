package any.data.entity

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

typealias ServiceConfigs = List<ServiceConfig>

fun ServiceConfigs.updateValuesFrom(other: ServiceConfigs?): ServiceConfigs {
    if (other.isNullOrEmpty()) return this
    val localConfigValues = other.associate { it.key to it.value }
    return map { it.copy(value = localConfigValues[it.key]) }
}

@Immutable
@JsonClass(generateAdapter = true)
data class ServiceConfig(
    val name: String,
    val key: String,
    val type: ServiceConfigType,
    val description: String? = null,
    val required: Boolean = false,
    val visibleToUser: Boolean = true,
    val value: ServiceConfigValue? = null,
    val extras: Any? = null,
) {
    @Json(ignore = true)
    val options: List<ServiceConfigOption>? = optionsFromExtras()

    @Json(ignore = true)
    val cookiesRequestUrl: String? = fieldFromMapExtras("requestUrl")

    @Json(ignore = true)
    val cookiesTargetUrl: String? = fieldFromMapExtras("targetUrl")

    @Json(ignore = true)
    val cookiesUserAgent: String? = fieldFromMapExtras("userAgent")

    @Suppress("unchecked_cast")
    private fun optionsFromExtras(): List<ServiceConfigOption>? {
        if (extras == null) return null
        val extraList = extras as? List<Map<String, String>> ?: return null
        return extraList.mapNotNull {
            val name = it["name"]
            val value = it["value"]
            if (name != null && value != null) {
                ServiceConfigOption(name, value)
            } else {
                null
            }
        }
    }

    @Suppress("unchecked_cast")
    private fun fieldFromMapExtras(name: String): String? {
        return (extras as? Map<String, String>)?.get(name)
    }
}

data class ServiceConfigOption(
    val name: String,
    val value: String,
)

sealed interface ServiceConfigValue {
    val stringValue: kotlin.String

    @JvmInline
    value class Boolean(val value: kotlin.Boolean) : ServiceConfigValue {
        override val stringValue: kotlin.String get() = value.toString()
    }

    @JvmInline
    value class Double(val value: kotlin.Double) : ServiceConfigValue {
        override val stringValue: kotlin.String get() = value.toString()
    }

    @JvmInline
    value class String(val value: kotlin.String) : ServiceConfigValue {
        override val stringValue: kotlin.String get() = value
    }

    @JsonClass(generateAdapter = true)
    class CookiesAndUa(
        val cookies: kotlin.String,
        val userAgent: kotlin.String,
    ) : ServiceConfigValue {
        override val stringValue: kotlin.String
            get() = any.data.json.Json.toJson(this, CookiesAndUa::class.java)
    }
}

@JsonClass(generateAdapter = false)
enum class ServiceConfigType {
    @Json(name = "boolean")
    Bool,

    @Json(name = "number")
    Number,

    @Json(name = "text")
    Text,

    @Json(name = "url")
    Url,

    @Json(name = "option")
    Option,

    @Json(name = "cookies")
    Cookies,

    @Json(name = "cookies_ua")
    CookiesAndUserAgent,
}
