package any.data.js

import android.content.Context
import any.data.FetchState
import any.data.entity.Comment
import any.data.entity.JsFetchResult
import any.data.entity.JsPageKey
import any.data.entity.JsPagedResult
import any.data.entity.JsPost
import any.data.entity.JsUser
import any.data.entity.PagedResult
import any.data.entity.Post
import any.data.entity.ServiceManifest
import any.data.entity.User
import any.data.js.engine.JsEngine
import any.data.js.engine.evaluate
import any.data.js.plugin.LocalServiceConfigsUpdater
import any.data.js.plugin.LocalServiceManifestUpdater
import any.data.js.plugin.MemoryServiceConfigsUpdater
import any.data.js.plugin.MemoryServiceManifestUpdater
import any.data.js.plugin.ProgressPlugin
import any.data.repository.PostContentRepository
import any.data.repository.ServiceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class ServiceBridgeImpl(
    private val serviceRunner: ServiceRunner,
    private val postContentRepository: PostContentRepository,
    private val serviceRepository: ServiceRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
) : ServiceBridge {
    override fun fetchUserById(
        service: ServiceManifest,
        id: String,
    ): Flow<FetchState<User>> = channelFlow {
        callFetchFunc(service) {
            val code = """
                const params = {
                    userId: "${id.escape()}",
                };
                const feature = service.features.user;
                feature.fetchById(params)
            """.trimIndent()

            // Run js
            val jsResult = evaluate<JsFetchResult<JsUser>?>(code)
            if (jsResult == null) {
                send(FetchState.failure(Exception("Failed to fetch user, id: $id")))
                return@callFetchFunc
            }

            send(convertJsFetchUserResult(service, jsResult))

            channel.close()
        }
    }

    override fun fetchUserByUrl(
        service: ServiceManifest,
        url: String,
    ): Flow<FetchState<User>> = channelFlow {
        callFetchFunc(service) {
            val code = """
                const params = {
                    userUrl: "${url.escape()}",
                };
                const feature = service.features.user;
                JSON.stringify(feature.fetchByUrl(params))
            """.trimIndent()

            // Run js
            val jsResult = evaluate<JsFetchResult<JsUser>?>(code)
            if (jsResult == null) {
                send(FetchState.failure(Exception("Failed to fetch user, url: $url")))
                return@callFetchFunc
            }

            send(convertJsFetchUserResult(service, jsResult))

            channel.close()
        }
    }

    override fun fetchUserPosts(
        service: ServiceManifest,
        userId: String,
        pageKey: JsPageKey?
    ): Flow<PagedPostsFetchState> = channelFlow {
        callFetchFunc(service) {
            val code = """
                const params = {
                    userId: "${userId.escape()}",
                    pageKey: ${pageKey.toJsValue()},
                };
                const feature = service.features.user;
                feature.fetchPosts(params)
            """.trimIndent()

            // Run js
            val jsResult = evaluate<JsPagedResult<List<JsPost>>?>(code)
            if (jsResult == null) {
                channel.send(FetchState.failure(Exception("Failed to fetch user posts")))
                return@callFetchFunc
            }

            send(convertJsPostPagedResult(service, jsResult))

            channel.close()
        }
    }

    override fun fetchLatestPosts(
        service: ServiceManifest,
        pageKey: JsPageKey?
    ): Flow<PagedPostsFetchState> = channelFlow {
        callFetchFunc(service) {
            val code = """
                const params = {
                    pageKey: ${pageKey.toJsValue()},
                };
                const feature = service.features.post;
                feature.fetchFreshList(params)
            """.trimIndent()

            val jsResult = evaluate<JsPagedResult<List<JsPost>>?>(code)

            if (jsResult == null) {
                channel.send(FetchState.failure(Exception("Failed to fetch latest posts")))
                return@callFetchFunc
            }

            send(convertJsPostPagedResult(service, jsResult))

            channel.close()
        }
    }

    override fun fetchPost(
        service: ServiceManifest,
        postUrl: String
    ): Flow<FetchState<Post?>> = channelFlow {
        callFetchFunc(service) {
            val code = """
                const params = {
                    url: "$postUrl",
                };
                const feature = service.features.post;
                feature.fetch(params)
            """.trimIndent()

            // Run js
            val jsResult = evaluate<JsFetchResult<JsPost>?>(code)
            if (jsResult == null) {
                send(FetchState.failure(Exception("Failed to load post")))
                return@callFetchFunc
            }

            if (jsResult.isOk()) {
                val jsPost = jsResult.data!!
                val post = jsPostToPost(service, jsPost)
                send(FetchState.success(value = post, isRemote = true))
            } else {
                val errorMessage = jsResult.error ?: "No data passed from js"
                send(FetchState.failure(error = Exception(errorMessage)))
            }

            channel.close()
        }
    }

    override fun searchPosts(
        service: ServiceManifest,
        query: String,
        pageKey: JsPageKey?
    ): Flow<PagedPostsFetchState> = channelFlow {
        callFetchFunc(service) {
            val code = """
                const params = {
                    query: "$query",
                    pageKey: ${pageKey.toJsValue()},
                };
                const feature = service.features.post;
                feature.search(params)
            """.trimIndent()

            // Run js
            val jsResult = evaluate<JsPagedResult<List<JsPost>>?>(code)
            if (jsResult == null) {
                send(FetchState.failure(Exception("Failed to load posts")))
                return@callFetchFunc
            }

            send(convertJsPostPagedResult(service, jsResult))

            channel.close()
        }
    }

    override fun fetchComments(
        service: ServiceManifest,
        postUrl: String,
        commentsKey: String,
        pageKey: JsPageKey?,
    ): Flow<PagedCommentsFetchState> = channelFlow {
        callFetchFunc(service) {
            val code = """
                const params = {
                    postUrl: "$postUrl",
                    loadKey: "$commentsKey",
                    pageKey: ${pageKey.toJsValue()},
                };
                const feature = service.features.post;
                feature.fetchComments(params)
            """.trimIndent()

            val jsResult = evaluate<JsPagedResult<List<Comment>>?>(code)
            if (jsResult == null) {
                send(FetchState.failure(Exception("Failed to load comments")))
                return@callFetchFunc
            }

            if (jsResult.isOk()) {
                val comments = jsResult.data!!.toList()
                val prevKey = jsResult.prevJsFetchKey()
                val nextKey = jsResult.nextJsFetchKey()
                val result = PagedResult(
                    data = comments,
                    prevKey = prevKey,
                    nextKey = nextKey,
                )
                send(FetchState.success(value = result, isRemote = true))
            } else {
                val errorMessage = jsResult.error ?: "No data passed from js"
                send(FetchState.failure(error = Exception(errorMessage)))
            }

            channel.close()
        }
    }

    override fun isSearchable(
        service: ServiceManifest
    ): Flow<Boolean> = flow {
        val isSearchable = serviceRunner.runSafely(
            service = service,
            manifestUpdater = MemoryServiceManifestUpdater(
                latest = { service },
                update = {},
            ),
            configsUpdater = MemoryServiceConfigsUpdater(
                latest = { service },
                update = {},
            ),
        ) {
            val testCode = """
                const feature = service.features.post;
                feature != null && typeof feature.search === "function" 
            """.trimIndent()
            evaluate<Boolean>(testCode) == true
        }
        emit(isSearchable.getOrDefault(false))
    }

    private fun convertJsFetchUserResult(
        service: ServiceManifest,
        result: JsFetchResult<JsUser>,
    ): FetchState<User> {
        return if (result.isOk()) {
            val jsUser = result.data!!
            val user = User.fromJsUser(serviceId = service.id, jsUser = jsUser)
            FetchState.success(value = user, isRemote = true)
        } else {
            val errorMessage = result.error ?: "Unknown error from js"
            FetchState.failure(error = Exception(errorMessage))
        }
    }

    private suspend fun convertJsPostPagedResult(
        service: ServiceManifest,
        result: JsPagedResult<List<JsPost>>,
    ): PagedPostsFetchState {
        return if (result.isOk()) {
            val jsPosts = result.data!!
            val createdAt = System.currentTimeMillis() + jsPosts.size - 1
            val posts: List<Post> = jsPosts
                .distinctBy { it.url }
                .mapIndexed { index, post ->
                    jsPostToPost(service, post).copy(createdAt = createdAt - index)
                }
            val prevKey = result.prevJsFetchKey()
            val nextKey = result.nextJsFetchKey()
            val ret = PagedResult(
                data = posts,
                prevKey = prevKey,
                nextKey = nextKey,
            )
            FetchState.success(value = ret, isRemote = true)
        } else {
            val errorMessage = result.error ?: "No data passed from js"
            FetchState.failure(error = Exception(errorMessage))
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun <T> ProducerScope<FetchState<T>>.callFetchFunc(
        service: ServiceManifest,
        block: suspend JsEngine.() -> Unit,
    ) {
        val progressPlugin = object : ProgressPlugin {
            override fun update(progress: Double, message: String?) {
                coroutineScope.launch {
                    send(FetchState.loading(progress.toFloat(), message))
                }
            }
        }
        val manifestUpdater = LocalServiceManifestUpdater(
            service = service,
            serviceRepository = serviceRepository,
            coroutineScope = coroutineScope,
        )
        val configsUpdater = LocalServiceConfigsUpdater(
            service = service,
            serviceRepository = serviceRepository,
            coroutineScope = coroutineScope,
        )
        val result = serviceRunner.runSafely(
            service = service,
            manifestUpdater = manifestUpdater,
            configsUpdater = configsUpdater,
        ) {
            set("__ANY_PROGRESS_PLUGIN__", ProgressPlugin::class.java, progressPlugin)
            block()
        }
        if (result.isSuccess) {
            return
        }
        val error = result.exceptionOrNull()
            ?: Exception("Unknown error occurred when executing js code")
        if (error is CancellationException) {
            throw error
        }
        if (!isClosedForSend) {
            send(FetchState.failure(error))
            channel.close()
        }
    }

    private suspend fun jsPostToPost(
        service: ServiceManifest,
        jsPost: JsPost,
    ): Post {
        postContentRepository.put(
            url = jsPost.url,
            elements = jsPost.content
        )
        return with(jsPost) {
            Post(
                title = title,
                url = url,
                media = media,
                serviceId = service.id,
                type = type ?: Post.Type.Article,
                rating = rating,
                summary = summary,
                date = date,
                author = author,
                authorId = authorId,
                avatar = avatar,
                category = category,
                tags = tags,
                commentCount = commentCount ?: 0,
                commentsKey = commentsKey,
                openInBrowser = openInBrowser == true,
                reference = reference?.let {
                    Post.Reference(
                        type = it.type,
                        post = jsPostToPost(service, it.post),
                    )
                },
            )
        }
    }

    companion object {
        private const val TAG = "RemotePostDataSource"

        @Volatile
        private var instance: ServiceBridgeImpl? = null

        fun getDefault(context: Context): ServiceBridgeImpl {
            return instance ?: synchronized(ServiceBridgeImpl::class) {
                instance ?: ServiceBridgeImpl(
                    serviceRunner = ServiceRunner.getDefault(context),
                    postContentRepository = PostContentRepository.getDefault(context),
                    serviceRepository = ServiceRepository.getDefault(context),
                ).also {
                    instance = it
                }
            }
        }
    }
}