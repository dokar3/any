package any.data.testing

import any.data.entity.Checksums
import any.data.entity.Post
import any.data.entity.PostsViewType
import any.data.entity.ServiceManifest
import java.util.UUID

object FakeData {
    val SERVICES = List(5) {
        ServiceManifest(
            id = "service.testing.$it",
            name = "Test service $it",
            description = "",
            developer = "Any",
            version = "1.0.0",
            minApiVersion = "0.1.0",
            maxApiVersion = null,
            isPageable = true,
            postsViewType = PostsViewType.List,
            mediaAspectRatio = "1:1",
            icon = null,
            headerImage = null,
            main = "main.js",
            mainChecksums = Checksums("", "", "", ""),
            languages = null,
            isEnabled = true,
            supportedPostUrls = null,
            configs = null,
            originalId = "",
            developerUrl = null,
            developerAvatar = null,
            homepage = null,
            changelog = null,
            themeColor = null,
            darkThemeColor = null,
            supportedUserUrls = listOf(),
            forceConfigsValidation = null,
            pageKeyOfPage2 = null,
        )
    }

    fun generatePosts(
        count: Int,
        serviceId: String = "",
        update: ((index: Int, post: Post) -> Post)? = null
    ): List<Post> {
        require(count >= 0)
        val now = System.currentTimeMillis()
        return List(count) { index ->
            Post(
                title = "Post title",
                url = "https://the.post.url/${UUID.randomUUID()}",
                serviceId = serviceId,
                type = Post.Type.Article,
                createdAt = now + index,
            ).let {
                if (update != null) {
                    update(index, it)
                } else {
                    it
                }
            }
        }
            .sortedBy { it.title }
            .sortedByDescending { it.createdAt }
    }
}