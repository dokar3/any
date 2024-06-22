package any.data.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
internal data class JsPost(
    val title: String,
    val url: String,
    val type: Post.Type? = null,
    val media: List<Post.Media>? = null,
    val rating: String? = null,
    val summary: String? = null,
    val date: String? = null,
    val author: String? = null,
    val authorId: String? = null,
    val avatar: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val content: List<ContentElement>? = null,
    val commentCount: Int? = 0,
    val commentsKey: String? = null,
    val openInBrowser: Boolean? = false,
    val reference: Reference? = null,
) {
    @JsonClass(generateAdapter = true)
    data class Reference(
        val type: Post.Reference.Type,
        val post: JsPost,
    )
}