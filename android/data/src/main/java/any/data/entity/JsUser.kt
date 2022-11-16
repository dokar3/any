package any.data.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class JsUser(
    val id: String,
    val name: String,
    val alternativeName: String?,
    val url: String?,
    val avatar: String?,
    val banner: String?,
    val description: String?,
    val followerCount: Int?,
    val followingCount: Int?,
    val postCount: Int?,
)
