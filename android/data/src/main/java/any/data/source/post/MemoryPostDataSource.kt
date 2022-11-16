package any.data.source.post

import any.base.cache.MemoryCache
import any.data.entity.Post
import any.data.entity.ServiceManifest

internal object MemoryPostDataSource {
    private val cache = MemoryCache<String, Post>(maxSize = 10000)

    fun get(serviceId: String, url: String): Post? {
        return cache.get(key(serviceId, url))
    }

    fun insert(post: Post) {
        cache.put(key(post), post)
    }

    fun insert(posts: List<Post>) {
        for (post in posts) {
            insert(post)
        }
    }

    fun remove(post: Post) {
        cache.remove(key(post))
    }

    fun update(post: Post) {
        cache.put(key(post), post)
    }

    fun clearFresh(service: ServiceManifest) {
        cache.getAll()
            .filter { it.serviceId == service.id && it.isInFresh() }
            .onEach { remove(it) }
    }

    fun clearUnused(service: ServiceManifest) {
        cache.getAll()
            .filter { it.serviceId == service.id && !it.isInUsing()}
            .onEach { remove(it) }
    }

    fun clear() {
        cache.clear()
    }

    private fun key(post: Post): String {
        return key(post.serviceId, post.url)
    }

    private fun key(serviceId: String, url: String): String {
        return serviceId + url
    }
}