package any.data.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ServiceResource(
    val type: Type,
    val path: String,
) {
    @JsonClass(generateAdapter = false)
    enum class Type {
        @Json(name = "main")
        Main,

        @Json(name = "icon")
        Icon,

        @Json(name = "header_image")
        HeaderImage,

        @Json(name = "changelog")
        Changelog,
    }
}