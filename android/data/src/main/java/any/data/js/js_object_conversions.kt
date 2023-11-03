package any.data.js

import any.data.entity.CookiesUaValue
import any.data.entity.JsPageKey
import any.data.entity.JsType
import any.data.entity.ServiceConfig
import any.data.entity.ServiceManifest
import any.data.entity.value

fun ServiceManifest.toJsObject(): JsonObject = json {
    "id" eq id
    "name" eq name
    "description" eq description
    "developer" eq developer
    "developerUrl" eq developerUrl
    "developerAvatar" eq developerAvatar
    "homepage" eq homepage
    "version" eq version
    "minApiVersion" eq minApiVersion
    "maxApiVersion" eq maxApiVersion
    "mainChecksums" eq json {
        "md5" eq mainChecksums.md5
        "sha1" eq mainChecksums.sha1
        "sha256" eq mainChecksums.sha256
        "sha512" eq mainChecksums.sha512
    }
    "isPageable" eq isPageable
    "postsViewType" eq postsViewType?.value
    "mediaAspectRatio" eq mediaAspectRatio
    "icon" eq icon
    "headerImage" eq headerImage
    "themeColor" eq themeColor
    "darkThemeColor" eq darkThemeColor
    "supportedPostUrls" eq supportedPostUrls
    "supportedUserUrls" eq supportedUserUrls
    "forceConfigsValidation" eq forceConfigsValidation
}

fun List<ServiceConfig>?.toJsObject(): JsonObject = json {
    val fields = this@toJsObject ?: emptyList()
    for (field in fields) {
        val key = field.key
        when (val value = field.value) {
            null -> {
                key eq null
            }

            is Boolean -> {
                key eq value
            }

            is Double -> {
                key eq value
            }

            is String -> {
                key eq value
            }

            is CookiesUaValue -> {
                key eq json {
                    "cookies" eq value.cookies
                    "userAgent" eq value.userAgent
                }
            }

            else -> throw IllegalArgumentException(
                "Unsupported value type: ${value::class.java}"
            )
        }
    }
}

fun JsPageKey?.toJsValue(): String {
    return when (this?.type) {
        JsType.String -> "\"${value.toString().escape()}\""
        JsType.Number -> value.toString()
        JsType.Boolean -> value.toString()
        JsType.Null, null -> "null"
        JsType.Undefined -> "undefined"
    }
}
