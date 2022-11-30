package any.data.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import any.data.entity.Post.Media.Type
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
@Entity(primaryKeys = ["url", "serviceId"])
data class Post(
    val title: String,
    val url: String,
    val serviceId: String,
    val type: Type?,
    val media: List<Media>? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val rating: String? = null,
    val date: String? = null,
    val summary: String? = null,
    val author: String? = null,
    val authorId: String? = null,
    val avatar: String? = null,
    val category: String? = null,
    val tags: List<String>? = null,
    val orderInFresh: Int = -1,
    val orderInProfile: Int = -1,
    val readPosition: Int = 0,
    val collectedAt: Long = -1L,
    val lastReadAt: Long = -1L,
    val downloadAt: Long = -1L,
    val folder: String? = null,
    val commentCount: Int = 0,
    val commentsKey: String? = null,
    val openInBrowser: Boolean = false,
    val reference: Reference? = null,
) {
    fun collect(): Post {
        return copy(collectedAt = System.currentTimeMillis())
    }

    fun discard(): Post {
        return copy(collectedAt = -1L)
    }

    fun markInDownload(): Post {
        return if (isInDownload()) this else copy(downloadAt = System.currentTimeMillis())
    }

    fun markUnDownload(): Post {
        return copy(downloadAt = -1L)
    }

    fun isInUsing() = isInFresh() || isInProfile() || isCollected() || isInDownload()

    fun isCollected() = collectedAt > 0L

    fun isInFresh() = orderInFresh >= 0

    fun isInProfile() = orderInProfile >= 0

    fun isInDownload() = downloadAt > 0L

    @JsonClass(generateAdapter = false)
    enum class Type {
        @Json(name = "article")
        Article,

        @Json(name = "comic")
        Comic,
    }

    @Immutable
    @JsonClass(generateAdapter = true)
    data class Media(
        val type: Type,
        val url: String,
        val aspectRatio: String? = null,
        /**
         * For [Type.Gif] and [Type.Video] type.
         */
        val thumbnail: String? = null,
    ) {
        fun thumbnailOrNull(): String? {
            return when (type) {
                Type.Photo -> thumbnail ?: url
                Type.Gif -> thumbnail ?: url
                Type.Video -> thumbnail
            }
        }

        @JsonClass(generateAdapter = false)
        enum class Type {
            @Json(name = "photo")
            Photo,

            @Json(name = "gif")
            Gif,

            @Json(name = "video")
            Video,
        }
    }

    @JsonClass(generateAdapter = true)
    data class Reference(
        val type: Type,
        val post: Post,
    ) {
        @JsonClass(generateAdapter = false)
        enum class Type {
            @Json(name = "repost")
            Repost,

            @Json(name = "quote")
            Quote,

            @Json(name = "reply")
            Reply,
        }
    }
}
