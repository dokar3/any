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
    val options: List<ServiceConfigOption>? = null,
    @Json(name = "requestUrl")
    val cookiesRequestUrl: String? = null,
    @Json(name = "targetUrl")
    val cookiesTargetUrl: String? = null,
    @Json(name = "userAgent")
    val cookiesUserAgent: String? = null,
)

@JsonClass(generateAdapter = true)
data class ServiceConfigOption(
    val name: String,
    val value: String,
)

sealed interface ServiceConfigValue {
    val stringValue: kotlin.String

    @JvmInline
    value class Boolean(val value: kotlin.Boolean) : ServiceConfigValue {
        override val stringValue: kotlin.String
            get() = value.toString()
    }

    @JvmInline
    value class Double(val value: kotlin.Double) : ServiceConfigValue {
        override val stringValue: kotlin.String
            get() = value.toString()
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
            get() = cookies + userAgent
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
