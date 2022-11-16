package any.domain.post

import any.data.ThumbAspectRatio
import any.data.entity.Post
import any.domain.entity.UiPost
import any.richtext.html.HtmlParser

fun Post.toUiPost(htmlParser: HtmlParser): UiPost {
    return UiPost.fromPost(post = this, htmlParser = htmlParser)
}

fun Iterable<Post>.toUiPosts(htmlParser: HtmlParser): List<UiPost> {
    return this.map { UiPost.fromPost(it, htmlParser) }
}

fun Iterable<UiPost>.containsRaw(post: Post): Boolean {
    return indexOfFirst { it.raw == post } != -1
}

fun UiPost.Media.Companion.fromPostMedia(media: Post.Media): UiPost.Media = with(media) {
    val thumb = when (type) {
        Post.Media.Type.Photo -> thumbnail ?: url
        Post.Media.Type.Gif -> thumbnail ?: url
        Post.Media.Type.Video -> thumbnail
    }
    val minAspectRatio: Float
    val maxAspectRatio: Float
    when (type) {
        Post.Media.Type.Photo,
        Post.Media.Type.Gif -> {
            minAspectRatio = ThumbAspectRatio.MIN_THUMB_ASPECT_RATIO
            maxAspectRatio = ThumbAspectRatio.MAX_THUMB_ASPECT_RATIO
        }

        Post.Media.Type.Video -> {
            minAspectRatio = ThumbAspectRatio.MIN_VIDEO_THUMB_ASPECT_RATIO
            maxAspectRatio = ThumbAspectRatio.MAX_VIDEO_THUMB_ASPECT_RATIO
        }
    }
    val ratio = aspectRatio?.let {
        ThumbAspectRatio.parse(
            aspectRatio = it,
            min = minAspectRatio,
            max = maxAspectRatio,
        )
    }
    return UiPost.Media(
        type = type,
        url = url,
        thumbnail = thumb,
        aspectRatio = ratio,
    )
}

fun UiPost.Companion.fromPost(
    post: Post,
    htmlParser: HtmlParser,
): UiPost = with(post) {
    return UiPost(
        raw = post,
        media = media?.map(UiPost.Media::fromPostMedia),
        summary = summary?.let(htmlParser::parse),
        reference = post.reference?.let {
            UiPost.Reference(
                type = it.type,
                post = fromPost(it.post, htmlParser),
            )
        },
    )
}
