package any.data.testing.source.post

import any.data.entity.Post
import any.data.source.post.LocalPostDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow

class TestLocalPostDataSource : LocalPostDataSource {
    private var allPosts = mutableListOf<Post>()

    private val postUpdateFlow = MutableSharedFlow<List<Post>>(extraBufferCapacity = 1)

    fun setPosts(posts: List<Post>) {
        allPosts = posts.toMutableList()
    }

    override fun postsUpdateFlow(): Flow<List<Post>> = postUpdateFlow

    override fun fetchPost(serviceId: String, postUrl: String): Flow<Post?> = flow {
        emit(allPosts.find { it.serviceId == serviceId && it.url == postUrl })
    }

    override fun fetchPosts(serviceId: String): Flow<List<Post>> = flow {
        emit(allPosts.filter { it.serviceId == serviceId })
    }

    override fun fetchFreshPosts(serviceId: String): Flow<List<Post>> = flow {
        emit(allPosts.filter { it.serviceId == serviceId && it.isInFresh() })
    }

    override fun fetchUserPosts(serviceId: String, userId: String): Flow<List<Post>> = flow {
        emit(allPosts.filter { it.serviceId == serviceId && it.authorId == userId })
    }

    override fun fetchUserInProfilePosts(serviceId: String, userId: String): Flow<List<Post>> =
        flow {
            emit(allPosts.filter { it.serviceId == serviceId && it.authorId == userId && it.isInProfile() })
        }

    override fun fetchCollectedPosts(): Flow<List<Post>> = flow {
        emit(allPosts.filter { it.isCollected() })
    }

    override fun fetchInDownloadPosts(): Flow<List<Post>> = flow {
        emit(allPosts.filter { it.isInDownloading() })
    }

    override suspend fun loadAll(): List<Post> {
        return allPosts.toList()
    }

    override suspend fun insert(post: Post) {
        allPosts.add(post)
    }

    override suspend fun insert(posts: List<Post>) {
        allPosts.addAll(posts)
    }

    override suspend fun update(post: Post) {
        val idx = allPosts.indexOfFirst { it.serviceId == post.serviceId && it.url == post.url }
        if (idx != -1) {
            allPosts[idx] = post
        }
    }

    override suspend fun update(posts: List<Post>) {
        posts.forEach { update(it) }
    }

    override suspend fun remove(post: Post) {
        allPosts.remove(post)
    }

    override suspend fun remove(serviceId: String, url: String) {
        val idx = allPosts.indexOfFirst { it.serviceId == serviceId && it.url == url }
        if (idx != -1) {
            allPosts.removeAt(idx)
        }
    }

    override suspend fun remove(posts: List<Post>) {
        allPosts.removeAll(posts)
    }

    override suspend fun clearFresh(serviceId: String) {
        val toRemove = allPosts.filter { it.serviceId == serviceId && it.isInFresh() }
        allPosts.removeAll(toRemove)
    }

    override suspend fun clearUnused(serviceId: String) {
        val toRemove = allPosts.filter { it.serviceId == serviceId && !it.isInUsing() }
        allPosts.removeAll(toRemove)
    }
}