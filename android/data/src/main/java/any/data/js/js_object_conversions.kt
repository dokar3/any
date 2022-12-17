package any.data.js

import any.data.entity.JsPageKey
import any.data.entity.JsType
import any.data.entity.ServiceConfig
import any.data.entity.ServiceConfigType
import any.data.entity.ServiceConfigValue
import any.data.entity.ServiceManifest
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun ServiceManifest.toJsObject(): JsObject = buildJsObject {
    "name" eq name
    "description" eq description
    "developer" eq developer
    "developerUrl" eq developerUrl
    "developerAvatar" eq developerAvatar
    "homepage" eq homepage
    "version" eq version
    "minApiVersion" eq minApiVersion
    "maxApiVersion" eq maxApiVersion
    "mainChecksums" eq buildJsObject {
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

fun List<ServiceConfig>?.toJsObject(): JsObject = buildJsObject {
    val fields = this@toJsObject ?: emptyList()
    for (field in fields) {
        val type = field.type
        val value = field.value
        if (value == null) {
            field.key eq JsObject.Null
            continue
        }
        when (type) {
            ServiceConfigType.Bool -> {
                checkConfigValue<ServiceConfigValue.Boolean>(value, type)
                field.key eq value.inner
            }

            ServiceConfigType.Number -> {
                checkConfigValue<ServiceConfigValue.Double>(value, type)
                field.key eq value.inner
            }

            ServiceConfigType.CookiesAndUserAgent -> {
                checkConfigValue<ServiceConfigValue.CookiesAndUa>(value, type)
                field.key eq buildJsObject {
                    "cookies" eq value.cookies
                    "userAgent" eq value.userAgent
                }
            }

            ServiceConfigType.Text,
            ServiceConfigType.Url,
            ServiceConfigType.Option,
            ServiceConfigType.Cookies -> {
                checkConfigValue<ServiceConfigValue.String>(value, type)
                field.key eq value.inner
            }
        }

    }
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
        val required = T::class.java
        val found = value::class.java
        val message = "The config value must match the type $type, " +
                "required: $required, found: $found"
        throw IllegalStateException(message)
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
