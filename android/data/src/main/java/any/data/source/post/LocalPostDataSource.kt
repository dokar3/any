package any.data.source.post

import any.data.entity.Post
import kotlinx.coroutines.flow.Flow

interface LocalPostDataSource {
    fun postsUpdateFlow(): Flow<List<Post>>

    fun fetchPost(serviceId: String, postUrl: String): Flow<Post?>

    fun fetchPosts(serviceId: String): Flow<List<Post>>

    fun fetchFreshPosts(serviceId: String): Flow<List<Post>>

    fun fetchUserPosts(serviceId: String, userId: String): Flow<List<Post>>

    fun fetchUserInProfilePosts(serviceId: String, userId: String): Flow<List<Post>>

    fun fetchCollectedPosts(): Flow<List<Post>>

    fun fetchInDownloadPosts(): Flow<List<Post>>

    suspend fun loadAll(): List<Post>

    suspend fun insert(post: Post)

    suspend fun insert(posts: List<Post>)

    suspend fun update(post: Post)

    suspend fun update(posts: List<Post>)

    suspend fun remove(post: Post)

    suspend fun remove(serviceId: String, url: String)

    suspend fun remove(posts: List<Post>)

    suspend fun clearFresh(serviceId: String)

    suspend fun clearUnused(serviceId: String)
}