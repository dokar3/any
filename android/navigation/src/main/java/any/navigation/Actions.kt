package any.navigation

import android.content.Context
import any.base.util.Intents
import any.data.entity.Post

fun navigateToMedia(
    handler: (NavEvent) -> Unit,
    context: Context,
    post: Post,
    index: Int,
) {
    val media = post.media
    if (media.isNullOrEmpty()) {
        return
    }
    val urls = media.map {
        when (it.type) {
            Post.Media.Type.Photo -> it.url
            Post.Media.Type.Gif -> it.url
            Post.Media.Type.Video -> null
        }
    }
    val nonNullUrls = urls.filterNotNull()
    if (nonNullUrls.isEmpty()) {
        return
    }
    val idx = index.coerceIn(0, urls.size - 1)
    if (urls[idx] != null) {
        var nullCountUntilIdx = 0
        for (i in 0 until idx) {
            if (urls[i] == null) {
                nullCountUntilIdx++
            }
        }
        val navEvent = NavEvent.PushImagePager(
            route = Routes.imagePager(
                title = post.title,
                currPage = idx - nullCountUntilIdx,
            ),
            images = nonNullUrls,
        )
        handler(navEvent)
    } else {
        navigateToPost(handler, context, post)
    }
}

fun navigateToPost(
    handler: (NavEvent) -> Unit,
    context: Context,
    post: Post,
) {
    if (post.openInBrowser) {
        Intents.openInBrowser(context, post.url)
    } else {
        handler(navPushEvent(Routes.post(post.url, post.serviceId)))
    }
}

fun navigateToUser(
    handler: (NavEvent) -> Unit,
    serviceId: String,
    userId: String,
) {
    val route = Routes.userProfile(serviceId = serviceId, userId = userId)
    handler(navPushEvent(route))
}
