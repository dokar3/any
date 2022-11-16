package any.domain.post

import any.data.entity.Post

object PostMediaImageCollector {
    /**
     * Collect images from post's media.
     */
    fun collect(
        post: Post,
        thumbnailFirst: Boolean = true,
        collectReferencePost: Boolean = true,
    ): List<String> {
        val mediaList = post.media ?: emptyList()
        val images = mutableListOf<String>()
        for (media in mediaList) {
            val thumb = media.thumbnail
            val url = when (media.type) {
                Post.Media.Type.Photo -> media.url
                Post.Media.Type.Gif -> media.url
                Post.Media.Type.Video -> null
            }
            if (thumbnailFirst && !thumb.isNullOrEmpty()) {
                images.add(thumb)
            } else if (!url.isNullOrEmpty()) {
                images.add(url)
            }
        }

        val ref = post.reference
        if (ref != null && collectReferencePost) {
            val refImages = collect(
                post = ref.post,
                thumbnailFirst = thumbnailFirst,
                collectReferencePost = true,
            )
            images.addAll(refImages)
        }

        return images
    }
}