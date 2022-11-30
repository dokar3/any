package any.data.source.post

import android.content.Context
import any.data.db.AppDatabase
import any.data.db.PostDao
import any.data.entity.Post
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

class LocalPostDataSourceImpl(
    private val postDao: PostDao,
) : LocalPostDataSource {
    private val _postsUpdateFlow = MutableSharedFlow<List<Post>>(extraBufferCapacity = 1)

    override fun postsUpdateFlow(): Flow<List<Post>> {
        return _postsUpdateFlow.distinctUntilChanged()
    }

    override fun fetchPosts(serviceId: String): Flow<List<Post>> = flow {
        val posts = postDao.getByServiceId(serviceId)
        emit(posts)
    }

    override fun fetchFreshPosts(serviceId: String): Flow<List<Post>> = flow {
        emit(postDao.getFresh(serviceId))
    }

    override fun fetchUserPosts(
        serviceId: String,
        userId: String,
    ): Flow<List<Post>> = flow {
        emit(postDao.getUserPosts(serviceId, userId))
    }

    override fun fetchUserInProfilePosts(
        serviceId: String,
        userId: String,
    ): Flow<List<Post>> = flow {
        emit(postDao.getInProfile(serviceId, userId))
    }

    override fun fetchCollectedPosts(): Flow<List<Post>> = flow {
        emit(postDao.getCollected())
    }

    override fun fetchInDownloadPosts(): Flow<List<Post>> = flow {
        emit(postDao.getInDownload())
    }

    override fun fetchPost(serviceId: String, postUrl: String): Flow<Post?> = flow {
        emit(postDao.get(serviceId, postUrl))
    }

    override suspend fun loadAll(): List<Post> {
        return postDao.getAll()
    }

    override suspend fun update(post: Post) {
        postDao.update(post)
        _postsUpdateFlow.emit(listOf(post))
    }

    override suspend fun update(posts: List<Post>) {
        postDao.update(posts)
        _postsUpdateFlow.emit(posts)
    }

    override suspend fun insert(post: Post) {
        postDao.add(post)
        _postsUpdateFlow.emit(listOf(post))
    }

    override suspend fun insert(posts: List<Post>) {
        postDao.add(posts)
        _postsUpdateFlow.emit(posts)
    }

    override suspend fun remove(post: Post) {
        postDao.remove(post)
    }

    override suspend fun remove(posts: List<Post>) {
        postDao.remove(posts)
    }

    override suspend fun remove(serviceId: String, url: String) {
        postDao.remove(serviceId, url)
    }

    override suspend fun clearFresh(serviceId: String) {
        postDao.clearFresh(serviceId)
    }

    override suspend fun clearUnused(serviceId: String) {
        postDao.clearUnused(serviceId)
    }

    companion object {
        @Volatile
        private var instance: LocalPostDataSourceImpl? = null

        fun getDefault(context: Context): LocalPostDataSourceImpl {
            return instance ?: synchronized(LocalPostDataSourceImpl::class) {
                instance ?: LocalPostDataSourceImpl(
                    postDao = AppDatabase.get(context).postDao(),
                ).also { instance = it }
            }
        }
    }
}