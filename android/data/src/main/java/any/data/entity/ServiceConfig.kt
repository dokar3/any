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

class ServiceConfigValue(value: Any?) {
    val text: String? = value?.toString()

    fun boolOrNull(): Boolean? {
        return text?.toBooleanStrictOrNull()
    }

    fun boolOrDefault(defaultValue: Boolean): Boolean {
        return text?.toBooleanStrictOrNull() ?: defaultValue
    }

    fun doubleOrNull(): Double? {
        return text?.toDoubleOrNull()
    }

    fun doubleOrNull(defaultValue: Double): Double {
        return text?.toDoubleOrNull() ?: defaultValue
    }

    override fun toString(): String {
        return text.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ServiceConfigValue

        if (text != other.text) return false

        return true
    }

    override fun hashCode(): Int {
        return text?.hashCode() ?: 0
    }

    @JsonClass(generateAdapter = true)
    data class UaAndCookies(
        val userAgent: String,
        val cookies: String,
    )
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
