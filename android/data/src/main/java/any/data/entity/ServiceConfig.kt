package any.data.entity

import androidx.compose.runtime.Immutable
import com.squareup.moshi.JsonClass

typealias ServiceConfigs = List<ServiceConfig>

fun ServiceConfigs.updateValuesFrom(other: ServiceConfigs?): ServiceConfigs {
    if (other.isNullOrEmpty()) return this
    val values = other.associate { it.key to it.value }
    return map { it.copy(value = values[it.key]) }
}

@Immutable
sealed class ServiceConfig(
    open val name: String,
    open val key: String,
    open val type: String,
    open val description: String? = null,
    open val required: Boolean = false,
    open val visibleToUser: Boolean = true,
) {
    abstract fun copy(value: Any?): ServiceConfig

    @JsonClass(generateAdapter = true)
    @Immutable
    data class Bool(
        override val name: String,
        override val key: String,
        override val type: String,
        override val description: String?,
        override val required: Boolean = false,
        override val visibleToUser: Boolean = true,
        val value: Boolean?,
    ) : ServiceConfig(
        name = name,
        key = key,
        type = type,
        description = description,
        required = required,
        visibleToUser = visibleToUser,
    ) {
        override fun copy(value: Any?): Bool {
            require(value is Boolean?) {
                val type = value?.let { it::class.java.simpleName }
                "copy() requires a Boolean value, but a(n) $type is found"
            }
            return copy(value = value)
        }
    }

    @JsonClass(generateAdapter = true)
    @Immutable
    data class Number(
        override val name: String,
        override val key: String,
        override val type: String,
        override val description: String?,
        override val required: Boolean = false,
        override val visibleToUser: Boolean = true,
        val value: Double?,
    ) : ServiceConfig(
        name = name,
        key = key,
        type = type,
        description = description,
        required = required,
        visibleToUser = visibleToUser,
    ) {
        override fun copy(value: Any?): Number {
            require(value is Double?) {
                val type = value?.let { it::class.java.simpleName }
                "copy() requires a Double value, but a(n) $type is found"
            }
            return copy(value = value)
        }
    }

    @JsonClass(generateAdapter = true)
    @Immutable
    data class Text(
        override val name: String,
        override val key: String,
        override val type: String,
        override val description: String?,
        override val required: Boolean = false,
        override val visibleToUser: Boolean = true,
        val value: String?,
    ) : ServiceConfig(
        name = name,
        key = key,
        type = type,
        description = description,
        required = required,
        visibleToUser = visibleToUser,
    ) {
        override fun copy(value: Any?): Text {
            require(value is String?) {
                val type = value?.let { it::class.java.simpleName }
                "copy() requires a String value, but a(n) $type is found"
            }
            return copy(value = value)
        }
    }

    @JsonClass(generateAdapter = true)
    @Immutable
    data class Url(
        override val name: String,
        override val key: String,
        override val type: String,
        override val description: String?,
        override val required: Boolean = false,
        override val visibleToUser: Boolean = true,
        val value: String?,
    ) : ServiceConfig(
        name = name,
        key = key,
        type = type,
        description = description,
        required = required,
        visibleToUser = visibleToUser,
    ) {
        override fun copy(value: Any?): Url {
            require(value is String?) {
                val type = value?.let { it::class.java.simpleName }
                "copy() requires a String value, but a(n) $type is found"
            }
            return copy(value = value)
        }
    }

    @JsonClass(generateAdapter = true)
    @Immutable
    data class Option(
        override val name: String,
        override val key: String,
        override val type: String,
        override val description: String?,
        override val required: Boolean = false,
        override val visibleToUser: Boolean = true,
        val value: String?,
        val options: List<ServiceConfigOption>,
    ) : ServiceConfig(
        name = name,
        key = key,
        type = type,
        description = description,
        required = required,
        visibleToUser = visibleToUser,
    ) {
        override fun copy(value: Any?): Option {
            require(value is String?) {
                val type = value?.let { it::class.java.simpleName }
                "copy() requires a String value, but a(n) $type is found"
            }
            return copy(value = value)
        }
    }

    @JsonClass(generateAdapter = true)
    @Immutable
    data class Cookies(
        override val name: String,
        override val key: String,
        override val type: String,
        override val description: String?,
        override val required: Boolean = false,
        override val visibleToUser: Boolean = true,
        val value: String?,
        val requestUrl: String,
        val targetUrl: String,
        val userAgent: String?,
    ) : ServiceConfig(
        name = name,
        key = key,
        type = type,
        description = description,
        required = required,
        visibleToUser = visibleToUser,
    ) {
        override fun copy(value: Any?): Cookies {
            require(value is String?) {
                val type = value?.let { it::class.java.simpleName }
                "copy() requires a String value, but a(n) $type is found"
            }
            return copy(value = value)
        }
    }

    @JsonClass(generateAdapter = true)
    @Immutable
    data class CookiesUa(
        override val name: String,
        override val key: String,
        override val type: String,
        override val description: String?,
        override val required: Boolean = false,
        override val visibleToUser: Boolean = true,
        val value: CookiesUaValue?,
        val requestUrl: String,
        val targetUrl: String,
        val userAgent: String?,
    ) : ServiceConfig(
        name = name,
        key = key,
        type = type,
        description = description,
        required = required,
        visibleToUser = visibleToUser,
    ) {
        override fun copy(value: Any?): CookiesUa {
            require(value is CookiesUaValue?) {
                val type = value?.let { it::class.java.simpleName }
                "copy() requires a CookiesUaValue value, but a(n) $type is found"
            }
            return copy(value = value)
        }
    }
}

val ServiceConfig.value: Any?
    get() = when (this) {
        is ServiceConfig.Bool -> value
        is ServiceConfig.Cookies -> value
        is ServiceConfig.CookiesUa -> value
        is ServiceConfig.Number -> value
        is ServiceConfig.Option -> value
        is ServiceConfig.Text -> value
        is ServiceConfig.Url -> value
    }

@JsonClass(generateAdapter = true)
data class CookiesUaValue(
    val cookies: String,
    val userAgent: String,
) {
    override fun toString(): String {
        return """{"cookies:"$cookies",userAgent:"$userAgent"}"""
    }
}

@JsonClass(generateAdapter = true)
data class ServiceConfigOption(
    val name: String,
    val value: String,
)
