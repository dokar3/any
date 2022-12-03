package any.data.js

import any.data.entity.Checksums
import any.data.entity.JsPageKey
import any.data.entity.JsType
import any.data.entity.ServiceConfig
import any.data.entity.ServiceConfigType
import any.data.entity.ServiceManifest

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
    appendStringField("sha512", sha512)
    append('}')
}

private fun StringBuilder.appendField(name: String, value: Any?) {
    append(name)
    append(':')
    append(value)
    append(",")
}

private fun StringBuilder.appendStringField(name: String, value: String?) {
    if (value != null) {
        append(name)
        append(":\"")
        append(value.escape())
        append("\",")
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
                    append(value.boolOrNull())
                }

                ServiceConfigType.Number -> {
                    append(value.doubleOrNull())
                }

                ServiceConfigType.CookiesAndUserAgent -> {
                    append(value)
                }

                else -> {
                    append('"')
                    append(value.toString().escape())
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

fun JsPageKey?.toJsObject(): String {
    return when (this?.type) {
        JsType.String -> "\"${value.toString().escape()}\""
        JsType.Number -> value.toString()
        JsType.Boolean -> value.toString()
        JsType.Null, null -> "null"
        JsType.Undefined -> "undefined"
    }
}
