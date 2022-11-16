package any.data.entity

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
data class ContentElement(
    val type: ContentElementType,
    val value: String?,
) {
    @JsonClass(generateAdapter = true)
    data class Image(
        val url: String,
        val aspectRatio: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Video(
        val url: String,
        val thumbnail: String?,
        val aspectRatio: String?,
    )

    @JsonClass(generateAdapter = true)
    data class Carousel(
        val items: List<Item>,
        val aspectRatio: String?,
    ) {
        @JsonClass(generateAdapter = true)
        data class Item(
            val image: String?,
            val video: String?,
        )
    }
}

@JsonClass(generateAdapter = false)
enum class ContentElementType {
    @Json(name = "text")
    Text,

    @Json(name = "html")
    Html,

    @Json(name = "image")
    Image,

    @Json(name = "full_width_image")
    FullWidthImage,

    @Json(name = "carousel")
    Carousel,

    @Json(name = "video")
    Video,

    @Json(name = "section")
    Section;

    fun isImage(): Boolean {
        return this == Image || this == FullWidthImage
    }
}