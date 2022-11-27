package any.data.entity

import androidx.compose.runtime.Immutable
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
data class Comment(
    val username: String = "",
    val content: String = "",
    val avatar: String? = null,
    val media: List<Post.Media>? = null,
    val date: Long = 0,
    val upvotes: Int = 0,
    val downvote: Int = 0,
    val replies: List<Comment>? = null,
)