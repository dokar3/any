package any.domain.entity

import androidx.compose.runtime.Immutable
import any.data.entity.Post
import any.richtext.RichContent

@Immutable
data class UiPost(
    val raw: Post,
    val media: List<Media>? = null,
    val summary: RichContent? = null,
    val reference: Reference? = null,
) {
    val title: String get() = raw.title
    val url: String get() = raw.url
    val serviceId: String get() = raw.serviceId
    val type: Post.Type? get() = raw.type
    val createdAt: Long get() = raw.createdAt
    val rating: String? get() = raw.rating
    val date: String? get() = raw.date
    val author: String? get() = raw.author
    val authorId: String? get() = raw.authorId
    val avatar: String? get() = raw.avatar
    val category: String? get() = raw.category
    val tags: List<String>? get() = raw.tags
    val orderInFresh: Int get() = raw.orderInFresh
    val orderInProfile: Int get() = raw.orderInProfile
    val readPosition: Int get() = raw.readPosition
    val collectedAt: Long get() = raw.createdAt
    val lastReadAt: Long get() = raw.lastReadAt
    val downloadAt: Long get() = raw.downloadAt
    val folder: String? get() = raw.folder
    val commentCount: Int get() = raw.commentCount
    val commentsKey: String? get() = raw.commentsKey
    val openInBrowser: Boolean get() = raw.openInBrowser

    fun copy(
        title: String = this.title,
        url: String = this.url,
        serviceId: String = this.serviceId,
        type: Post.Type? = this.type,
        createdAt: Long = this.createdAt,
        rating: String? = this.rating,
        date: String? = this.date,
        author: String? = this.author,
        authorId: String? = this.authorId,
        avatar: String? = this.avatar,
        category: String? = this.category,
        tags: List<String>? = this.tags,
        orderInFresh: Int = this.orderInFresh,
        orderInProfile: Int = this.orderInProfile,
        readPosition: Int = this.readPosition,
        collectedAt: Long = this.collectedAt,
        lastReadAt: Long = this.lastReadAt,
        downloadAt: Long = this.downloadAt,
        folder: String? = this.folder,
        commentCount: Int = this.commentCount,
        commentsKey: String? = this.commentsKey,
        openInBrowser: Boolean = this.openInBrowser,
        media: List<Media>? = this.media,
        summary: RichContent? = this.summary,
        reference: Reference? = this.reference,
    ): UiPost {
        val updatedRawPost = raw.copy(
            title = title,
            url = url,
            serviceId = serviceId,
            type = type,
            createdAt = createdAt,
            rating = rating,
            date = date,
            author = author,
            authorId = authorId,
            avatar = avatar,
            category = category,
            tags = tags,
            orderInFresh = orderInFresh,
            orderInProfile = orderInProfile,
            readPosition = readPosition,
            collectedAt = collectedAt,
            lastReadAt = lastReadAt,
            downloadAt = downloadAt,
            folder = folder,
            commentCount = commentCount,
            commentsKey = commentsKey,
            openInBrowser = openInBrowser,
        )
        return copy(
            raw = updatedRawPost,
            media = media,
            summary = summary,
            reference = reference,
        )
    }

    fun isCollected() = raw.isCollected()

    @Immutable
    data class Media(
        val type: Post.Media.Type,
        val url: String,
        val thumbnail: String?,
        val aspectRatio: Float?,
    ) {
        companion object
    }

    data class Reference(
        val type: Post.Reference.Type,
        val post: UiPost,
    )

    companion object
}