package any.data.js

import any.data.entity.Checksums
import any.data.entity.JsPageKey
import any.data.entity.JsType
import any.data.entity.ServiceConfig
import any.data.entity.ServiceConfigType
import any.data.entity.ServiceConfigValue
import any.data.entity.ServiceManifest
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun String.escape(): String {
    return replace("\\", "\\\\")
        .replace("\t", "\\t")
        .replace("\b", "\\b")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\'", "\\'")
        .replace("\"", "\\\"")
}

fun ServiceManifest.toJsManifestObject(): String = buildString {
    append('{')

    appendStringField("id", id)
    appendStringField("name", name)
    appendStringField("description", description)
    appendStringField("developer", developer)
    appendStringField("developerUrl", developerUrl)
    appendStringField("developerAvatar", developerAvatar)
    appendStringField("homepage", homepage)
    appendStringField("version", version)
    appendStringField("minApiVersion", minApiVersion)
    appendStringField("maxApiVersion", maxApiVersion)
    appendField("mainChecksums", mainChecksums.toJsObject())
    appendField("isPageable", isPageable)
    appendStringField("postsViewType", postsViewType?.value)
    appendStringField("mediaAspectRatio", mediaAspectRatio)
    appendStringField("icon", icon)
    appendStringField("headerImage", headerImage)
    appendStringField("themeColor", themeColor)
    appendStringField("darkThemeColor", darkThemeColor)
    appendStringArray("supportedPostUrls", supportedPostUrls)
    appendStringArray("supportedUserUrls", supportedUserUrls)
    appendField("forceConfigsValidation", forceConfigsValidation)

    append('}')
}

private fun Checksums.toJsObject(): String = buildString {
    append('{')
    appendStringField("md5", md5)
    appendStringField("sha1", sha1)
    appendStringField("sha256", sha256)
    appendStringField("sha512", sha512, appendComma = false)
    append('}')
}

private fun StringBuilder.appendField(name: String, value: Any?) {
    append(name)
    append(':')
    append(value)
    append(",")
}

private fun StringBuilder.appendStringField(
    name: String,
    value: String?,
    appendComma: Boolean = true,
) {
    if (value != null) {
        append(name)
        append(":\"")
        append(value.escape())
        if (appendComma) {
            append("\",")
        } else {
            append('"')
        }
    } else {
        append(name)
        append(":null,")
    }
}

private fun StringBuilder.appendStringArray(name: String, values: List<String>?) {
    if (values != null) {
        append(name)
        append(':')
        append('[')
        for (value in values) {
            append('"')
            append(value.escape())
            append('"')
            append(',')
        }
        append("],")
    } else {
        append(name)
        append(":null,")
    }
}

fun List<ServiceConfig>?.toJsObject(): String = buildString {
    append('{')
    val fields = this@toJsObject ?: emptyList()
    for (field in fields) {
        append(field.key)
        append(":")
        val type = field.type
        val value = field.value
        if (value != null) {
            when (type) {
                ServiceConfigType.Bool -> {
                    checkConfigValue<ServiceConfigValue.Boolean>(value, type)
                    append(value.stringValue)
                }

                ServiceConfigType.Number -> {
                    checkConfigValue<ServiceConfigValue.Double>(value, type)
                    append(value.stringValue)
                }

                ServiceConfigType.CookiesAndUserAgent -> {
                    checkConfigValue<ServiceConfigValue.CookiesAndUa>(value, type)
                    append('{')
                    appendStringField("cookies", value.cookies)
                    appendStringField("userAgent", value.userAgent, appendComma = false)
                    append('}')
                }

                else -> {
                    append('"')
                    append(value.stringValue.escape())
                    append('"')
                }
            }
        } else {
            append("null")
        }
        append(',')
    }
    append('}')
}

@OptIn(ExperimentalContracts::class)
private inline fun <reified T : ServiceConfigValue> checkConfigValue(
    value: ServiceConfigValue,
    type: ServiceConfigType,
) {
    contract {
        returns() implies (value is T)
    }
    if (value !is T) {
        val required = ServiceConfigValue.CookiesAndUa::class.java
        val found = value::class.java
        val message = "The config value must match the type $type, " +
                "required: $required, found: $found"
        throw IllegalStateException(message)
    }
}

fun JsPageKey?.toJsObject(): String {
    return when (this?.type) {
        JsType.String -> "\"${value.toString().escape()}\""
        JsType.Number -> value.toString()
        JsType.Boolean -> value.toString()
        JsType.Null, null -> "null"
        JsType.Undefined -> "undefined"
    }
}
