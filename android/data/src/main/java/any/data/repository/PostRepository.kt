package any.data.repository

import android.content.Context
import any.base.util.ifNullOrEmpty
import any.data.FetchState
import any.data.entity.JsPageKey
import any.data.entity.PagedResult
import any.data.entity.Post
import any.data.entity.ServiceManifest
import any.data.entity.User
import any.data.js.PagedCommentsFetchState
import any.data.js.PagedPostsFetchState
import any.data.js.ServiceBridge
import any.data.js.ServiceBridgeImpl
import any.data.source.post.LocalPostDataSource
import any.data.source.post.LocalPostDataSourceImpl
import any.data.source.post.MemoryPostDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.max

class PostRepository(
    private val localDataSource: LocalPostDataSource,
    private val serviceBridge: ServiceBridge,
    private val postContentRepository: PostContentRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * Memory post cache for storing temporary posts (search results, etc.).
     * Should update the cache on every read/write/update to keep posts in cache is fresh.
     */
    private val memoryPostDataSource = MemoryPostDataSource

    private val mutex = Mutex()

    fun fetchInitialUserPosts(
        service: ServiceManifest,
        user: User,
        sources: FetchSources,
    ): Flow<PagedPostsFetchState> = channelFlow {
        if (sources.contains(FetchSources.cache())) {
            val cachedUserPosts = localDataSource.fetchUserInProfilePosts(service.id, user.id).first()
            val result = PagedResult(
                data = cachedUserPosts,
                nextKey = user.pageKeyOfPage2,
            )
            send(FetchState.success(value = result, isRemote = false))
            if (cachedUserPosts.isNotEmpty() && sources.isOneShot()) {
                channel.close()
                return@channelFlow
            }
        }

        if (!sources.contains(FetchSources.remote())) {
            channel.close()
            return@channelFlow
        }

        fetchInitialPosts(
            localFlow = localDataSource.fetchPosts(service.id),
            remoteFlow = serviceBridge.fetchUserPosts(service = service, userId = user.id, pageKey = null),
            isInitial = { post -> post.isInProfile() && post.authorId == user.id },
            resetInitial = { posts -> posts.map { it.copy(orderInProfile = -1) } },
            setInitial = { posts ->
                posts.mapIndexed { idx, post -> post.copy(orderInProfile = idx) }
            },
        ) {
            channel.send(it)
        }

        channel.close()
    }

    fun fetchInitialPosts(
        service: ServiceManifest,
        strategy: FetchLatestStrategy
    ): Flow<PagedPostsFetchState> = channelFlow {
        val cachedInitialPosts = localDataSource.fetchFreshPosts(service.id).first()

        val skipRemote = strategy == FetchLatestStrategy.CacheOnly ||
                strategy == FetchLatestStrategy.RemoteIfEmptyCache &&
                cachedInitialPosts.isNotEmpty()
        if (skipRemote) {
            updateMemoryCache(posts = cachedInitialPosts)
            // Also return the cached page key of page 2
            val result = PagedResult(
                data = cachedInitialPosts,
                nextKey = service.pageKeyOfPage2,
            )
            send(FetchState.success(value = result, isRemote = false))
            channel.close()
            return@channelFlow
        }

        fetchInitialPosts(
            localFlow = localDataSource.fetchPosts(service.id),
            remoteFlow = serviceBridge.fetchLatestPosts(service = service, pageKey = null),
            isInitial = Post::isInFresh,
            resetInitial = { posts -> posts.map { it.copy(orderInFresh = -1) } },
            setInitial = { posts ->
                posts.mapIndexed { idx, post -> post.copy(orderInFresh = idx) }
            },
        ) {
            channel.send(it)
        }

        channel.close()
    }

    private suspend fun fetchInitialPosts(
        localFlow: Flow<List<Post>>,
        remoteFlow: Flow<PagedPostsFetchState>,
        isInitial: (Post) -> Boolean,
        resetInitial: (List<Post>) -> List<Post>,
        setInitial: (List<Post>) -> List<Post>,
        onReceive: suspend (PagedPostsFetchState) -> Unit,
    ) {
        remoteFlow
            .onEach {
                if (it !is FetchState.Success) {
                    onReceive(it)
                }
            }
            .mapNotNull { it as? FetchState.Success }
            .combine(localFlow) { fresh, cached ->
                val freshPosts = fresh.value.data
                val updatedPosts = updateAndCacheInitialPosts(
                    posts = freshPosts,
                    serviceCachedPosts = cached,
                    isInitial = isInitial,
                    resetInitial = resetInitial,
                    setInitial = setInitial,
                )
                updateMemoryCache(posts = updatedPosts)
                val result = PagedResult(
                    data = updatedPosts,
                    prevKey = fresh.value.prevKey,
                    nextKey = fresh.value.nextKey,
                )
                FetchState.success(value = result, isRemote = true)
            }
            .collect { onReceive(it) }
    }

    fun fetchMoreUserPosts(
        service: ServiceManifest,
        user: User,
        pageKey: JsPageKey,
    ): Flow<PagedPostsFetchState> {
        return fetchMorePosts(
            localFlow = localDataSource.fetchPosts(service.id),
            remoteFlow = serviceBridge.fetchUserPosts(service, user.id, pageKey),
        )
    }

    fun fetchMorePosts(
        service: ServiceManifest,
        key: JsPageKey,
    ): Flow<PagedPostsFetchState> {
        return fetchMorePosts(
            localFlow = localDataSource.fetchPosts(service.id),
            remoteFlow = serviceBridge.fetchLatestPosts(service, key),
        )
    }

    private fun fetchMorePosts(
        localFlow: Flow<List<Post>>,
        remoteFlow: Flow<PagedPostsFetchState>
    ): Flow<PagedPostsFetchState> {
        return remoteFlow.combine(localFlow) { fresh, cached ->
            if (fresh is FetchState.Success) {
                val freshPosts = fresh.value.data
                val updatedPosts = updateFieldsFromCachedPosts(freshPosts, cached)
                updateMemoryCache(posts = updatedPosts)
                val result = PagedResult(
                    data = updatedPosts,
                    prevKey = fresh.value.prevKey,
                    nextKey = fresh.value.nextKey,
                )
                FetchState.success(value = result, isRemote = true)
            } else {
                fresh
            }
        }
    }

    fun fetchPost(
        service: ServiceManifest,
        postServiceId: String,
        postUrl: String,
        strategy: FetchPostStrategy
    ): Flow<FetchState<Post?>> = channelFlow {
        val cachedPost = getPostFromCache(postServiceId, postUrl)
        if (strategy != FetchPostStrategy.RemoteOnly) {
            // Emit cached result
            send(FetchState.success(value = cachedPost, isRemote = false))
        }

        if (strategy == FetchPostStrategy.CacheOnly) {
            return@channelFlow
        }

        val skipRemote = strategy == FetchPostStrategy.RemoteIfMissingContent &&
                cachedPost != null && postContentRepository.contains(cachedPost.url)
        if (skipRemote) {
            return@channelFlow
        }

        serviceBridge.fetchPost(service, postUrl)
            .onEach {
                if (it !is FetchState.Success) {
                    // Emit loading and failure states
                    send(it)
                }
            }
            .mapNotNull { it as? FetchState.Success }
            .collect { fresh ->
                val freshPost = checkNotNull(fresh.value) {
                    "Can not load this post: $postUrl"
                }
                val cached = getPostFromCache(serviceId = service.id, postUrl = postUrl)
                val updatedPost = updateFreshPostFromCache(freshPost, cached)
                updateMemoryCache(post = updatedPost)
                send(FetchState.success(value = updatedPost, isRemote = true))
                localDataSource.insert(updatedPost)
                close()
            }
    }

    suspend fun loadCachedPost(serviceId: String, url: String): Post? {
        return localDataSource.fetchPost(serviceId, url).first()
    }

    fun loadCollectedPosts(): Flow<List<Post>> = flow {
        mutex.withLock {
            val posts = localDataSource.fetchCollectedPosts().first()
            updateMemoryCache(posts = posts)
            emit(posts)
        }
    }

    suspend fun isServiceSearchable(service: ServiceManifest): Boolean {
        return serviceBridge.isSearchable(service).first()
    }

    fun searchRemotePosts(
        service: ServiceManifest,
        query: String,
        key: JsPageKey?,
    ): Flow<PagedPostsFetchState> = channelFlow {
        serviceBridge
            .searchPosts(
                service = service,
                query = query,
                pageKey = key,
            )
            .onEach {
                if (it !is FetchState.Success) {
                    send(it)
                }
            }
            .mapNotNull { it as? FetchState.Success }
            .combine(localDataSource.fetchPosts(service.id)) { searchResult, cached ->
                val posts = searchResult.value.data
                val cachedPosts = cached.associateBy { it.url }
                val updatedPosts = updateFieldsFromCachedPosts(
                    posts = posts,
                    cachedPosts = cached,
                ).map {
                    it.copy(
                        orderInFresh = cachedPosts[it.url]?.orderInFresh ?: -1,
                        orderInProfile = cachedPosts[it.url]?.orderInProfile ?: -1,
                    )
                }
                updateMemoryCache(posts = updatedPosts)
                PagedResult(
                    data = updatedPosts,
                    prevKey = searchResult.value.prevKey,
                    nextKey = searchResult.value.nextKey,
                )
            }
            .collect { result ->
                send(FetchState.success(value = result, isRemote = true))
            }
    }

    fun fetchComments(
        service: ServiceManifest,
        postUrl: String,
        commentsKey: String,
        pageKey: JsPageKey?,
    ): Flow<PagedCommentsFetchState> {
        return serviceBridge.fetchComments(
            service = service,
            postUrl = postUrl,
            commentsKey = commentsKey,
            pageKey = pageKey,
        )
    }

    fun postsUpdateFlow(): Flow<List<Post>> {
        return localDataSource.postsUpdateFlow()
    }

    suspend fun collectPost(post: Post) {
        val cached = loadCachedPost(serviceId = post.serviceId, url = post.url)
        val orderInFresh = cached?.orderInFresh ?: post.orderInFresh
        val orderInProfile = cached?.orderInProfile ?: post.orderInProfile
        val updated = post.collect().copy(
            orderInFresh = orderInFresh,
            orderInProfile = orderInProfile,
        )
        updateMemoryCache(post = updated)
        localDataSource.insert(updated)
    }

    suspend fun discardPost(post: Post) {
        val cached = loadCachedPost(serviceId = post.serviceId, url = post.url)
        val orderInFresh = cached?.orderInFresh ?: post.orderInFresh
        val orderInProfile = cached?.orderInProfile ?: post.orderInProfile
        val updated = post.discard().copy(
            orderInFresh = orderInFresh,
            orderInProfile = orderInProfile,
        )
        updateMemoryCache(post = updated)
        localDataSource.insert(updated)
    }

    suspend fun updatePost(post: Post) {
        mutex.withLock {
            updateMemoryCache(post = post)
            localDataSource.update(post)
        }
    }

    suspend fun updatePosts(posts: List<Post>) {
        updateMemoryCache(posts = posts)
        localDataSource.update(posts)
    }

    suspend fun clearFresh(service: ServiceManifest) {
        memoryPostDataSource.clearFresh(service)
        localDataSource.clearFresh(service.id)
    }

    suspend fun clearUnused(service: ServiceManifest) {
        memoryPostDataSource.clearUnused(service)
        localDataSource.clearUnused(service.id)
    }

    suspend fun updatePostsFromCache(service: ServiceManifest, posts: List<Post>): List<Post> {
        val cached = localDataSource.fetchPosts(service.id).first()
        updateMemoryCache(posts = cached)
        return updateFieldsFromCachedPosts(posts, cached)
    }

    private suspend fun getPostFromCache(
        serviceId: String,
        postUrl: String,
    ): Post? {
        // 1. Check memory cache
        val memPost = memoryPostDataSource.get(serviceId, postUrl)
        if (memPost != null) {
            return memPost
        }
        // 2. Query db if not found in memory
        return localDataSource.fetchPost(serviceId, postUrl).first()
    }

    private fun updateFreshPostFromCache(
        fresh: Post,
        cached: Post?,
    ): Post {
        return if (cached != null) {
            // Update fields from cache
            val updated = fresh.copy(
                title = fresh.title.ifEmpty { cached.title },
                media = fresh.media.ifNullOrEmpty { cached.media },
                author = fresh.author.ifNullOrEmpty { cached.author },
                orderInFresh = cached.orderInFresh,
                orderInProfile = cached.orderInProfile,
                createdAt = cached.createdAt,
                collectedAt = cached.collectedAt,
                downloadAt = cached.downloadAt,
                commentsKey = fresh.commentsKey.ifNullOrEmpty { cached.commentsKey },
                tags = fresh.tags.ifNullOrEmpty { cached.tags },
                lastReadAt = cached.lastReadAt,
                readPosition = cached.readPosition,
                folder = cached.folder,
            )
            updated
        } else {
            fresh
        }
    }

    private fun updateMemoryCache(post: Post?) {
        if (post != null) {
            memoryPostDataSource.insert(post)
        }
    }

    private fun updateMemoryCache(posts: List<Post>?) {
        if (posts.isNullOrEmpty()) {
            return
        }
        memoryPostDataSource.insert(posts)
    }

    private suspend fun updateAndCacheInitialPosts(
        posts: List<Post>,
        serviceCachedPosts: List<Post>,
        isInitial: (Post) -> Boolean,
        resetInitial: (posts: List<Post>) -> List<Post>,
        setInitial: (posts: List<Post>) -> List<Post>,
    ): List<Post> = withContext(ioDispatcher) {
        if (serviceCachedPosts.isEmpty()) {
            // Nothing in cache, save and return
            localDataSource.insert(setInitial(posts))
            return@withContext posts
        }

        val now = System.currentTimeMillis()
        val newPostUrls = posts.map(Post::url).toHashSet()
        // Remove invalid cache
        val toRemove = serviceCachedPosts.filter {
            val isInvalid = !it.isInUsing() &&
                    !newPostUrls.contains(it.url) &&
                    now - it.createdAt > MAX_AGE_FOR_INVALID_CACHE
            isInvalid
        }
        localDataSource.remove(toRemove)
        // Remove the post content as well
        toRemove.forEach { postContentRepository.remove(it.url) }

        // Reset initial posts
        val toUpdate = resetInitial((serviceCachedPosts - toRemove.toSet()).filter(isInitial))
        localDataSource.update(toUpdate)

        val newPosts = setInitial(updateFieldsFromCachedPosts(posts, serviceCachedPosts))
        // Save new posts
        localDataSource.insert(newPosts)
        return@withContext newPosts
    }

    private fun updateFieldsFromCachedPosts(
        posts: List<Post>,
        cachedPosts: List<Post>
    ): List<Post> {
        if (cachedPosts.isEmpty()) {
            return posts
        }
        val cacheMap = hashMapOf<String, Post>()
        cachedPosts.fold(cacheMap) { acc, post ->
            acc[post.url] = post
            acc
        }

        val updatedPosts = mutableListOf<Post>()

        for (i in posts.indices) {
            val post = posts[i]
            val cached = cacheMap[post.url]

            if (cached == null) {
                updatedPosts.add(post)
                continue
            }

            val collectedAt = cached.collectedAt
            val rating = post.rating.ifNullOrEmpty { cached.rating }
            val date = post.date.ifNullOrEmpty { cached.date }
            val author = post.author.ifNullOrEmpty { cached.author }
            val category = post.category.ifNullOrEmpty { cached.category }
            val tags = post.tags ?: cached.tags
            val commentCount = max(post.commentCount, cached.commentCount)
            val commentsLoadKey = post.commentsKey.ifNullOrEmpty { cached.commentsKey }

            updatedPosts.add(
                post.copy(
                    rating = rating,
                    date = date,
                    author = author,
                    category = category,
                    tags = tags,
                    commentCount = commentCount,
                    commentsKey = commentsLoadKey,
                    orderInFresh = cached.orderInFresh,
                    orderInProfile = cached.orderInProfile,
                    lastReadAt = cached.lastReadAt,
                    downloadAt = cached.downloadAt,
                    collectedAt = collectedAt,
                    readPosition = cached.readPosition,
                    folder = cached.folder,
                )
            )
        }

        return updatedPosts
    }

    enum class FetchLatestStrategy {
        CacheOnly,
        RemoteOnly,
        RemoteIfEmptyCache
    }

    enum class FetchPostStrategy {
        CacheOnly,
        RemoteOnly,
        RemoteIfMissingContent
    }

    companion object {
        private const val TAG = "PostRepository"

        const val MAX_AGE_FOR_INVALID_CACHE = 24 * 60 * 60 * 1000L * 7 // 7 days

        @Volatile
        private var instance: PostRepository? = null

        fun getDefault(context: Context): PostRepository {
            return instance ?: synchronized(PostRepository::class) {
                instance ?: PostRepository(
                    localDataSource = LocalPostDataSourceImpl.getDefault(context),
                    serviceBridge = ServiceBridgeImpl.getDefault(context),
                    postContentRepository = PostContentRepository.getDefault(context),
                ).also {
                    instance = it
                }
            }
        }
    }
}