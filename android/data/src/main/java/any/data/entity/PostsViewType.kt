package any.data.entity

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class PostsViewType(val value: String) {
    @Json(name = "grid")
    Grid("grid"),

    @Json(name = "list")
    List("list"),

    @Json(name = "full_width")
    FullWidth("full-width"),

    @Json(name = "card")
    Card("card"),
}