package any.data.js

import android.content.Context
import any.base.log.Logger
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
import any.data.js.plugin.LocalServiceConfigsUpdater
import any.data.js.plugin.LocalServiceManifestUpdater
import any.data.js.plugin.MemoryServiceConfigsUpdater
import any.data.js.plugin.MemoryServiceManifestUpdater
import any.data.js.plugin.ProgressPlugin
import any.data.json.Json
import any.data.repository.PostContentRepository
import any.data.repository.ServiceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.intellij.lang.annotations.Language

class ServiceBridgeImpl(
    private val serviceRunner: ServiceRunner,
    private val postContentRepository: PostContentRepository,
    private val serviceRepository: ServiceRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val json: Json = Json,
) : ServiceBridge {
    override fun fetchUserById(
        service: ServiceManifest,
        id: String,
    ): Flow<FetchState<User>> = channelFlow {
        callFetchFunc(service) {
            @Language("JS")
            val code = """
                const params = {
                    userId: "${id.escape()}",
                };
                const feature = service.getFeature(AnyUserFeature);
                JSON.stringify(feature.fetchById(params))
            """.trimIndent()

            // Run js
            val ret = evaluate(code, String::class.java)
            if (ret == null) {
                send(FetchState.failure(Exception("Failed to fetch user, id: $id")))
                return@callFetchFunc
            }

            send(convertJsFetchUserResult(service, ret))

            channel.close()
        }
    }

    override fun fetchUserByUrl(
        service: ServiceManifest,
        url: String,
    ): Flow<FetchState<User>> = channelFlow {
        callFetchFunc(service) {
            @Language("JS")
            val code = """
                const params = {
                    userUrl: "${url.escape()}",
                };
                const feature = service.getFeature(AnyUserFeature);
                JSON.stringify(feature.fetchByUrl(params))
            """.trimIndent()

            // Run js
            val ret = evaluate(code, String::class.java)
            if (ret == null) {
                send(FetchState.failure(Exception("Failed to fetch user, url: $url")))
                return@callFetchFunc
            }

            send(convertJsFetchUserResult(service, ret))

            channel.close()
        }
    }

    override fun fetchUserPosts(
        service: ServiceManifest,
        userId: String,
        pageKey: JsPageKey?
    ): Flow<PagedPostsFetchState> = channelFlow {
        callFetchFunc(service) {
            @Language("JS")
            val code = """
                const params = {
                    userId: "${userId.escape()}",
                    pageKey: ${pageKey.toJsObject()},
                };
                const feature = service.getFeature(AnyUserFeature);
                const pagedResult = feature.fetchPosts(params);
                JSON.stringify(pagedResult)
            """.trimIndent()

            // Run js
            val result = evaluate(code, String::class.java)
            if (result == null) {
                channel.send(FetchState.failure(Exception("Failed to fetch user posts")))
                return@callFetchFunc
            }

            send(convertJsPostPagedResult(service, result))

            channel.close()
        }
    }

    override fun fetchLatestPosts(
        service: ServiceManifest,
        pageKey: JsPageKey?
    ): Flow<PagedPostsFetchState> = channelFlow {
        callFetchFunc(service) {
            @Language("JS")
            val code = """
                const params = {
                    pageKey: ${pageKey.toJsObject()},
                };
                const feature = service.getFeature(AnyPostFeature);
                const pagedResult = feature.fetchFreshList(params);
                JSON.stringify(pagedResult)
            """.trimIndent()

            // Run js
            val result = evaluate(code, String::class.java)
            if (result == null) {
                channel.send(FetchState.failure(Exception("Failed to fetch latest posts")))
                return@callFetchFunc
            }

            send(convertJsPostPagedResult(service, result))

            channel.close()
        }
    }

    override fun fetchPost(
        service: ServiceManifest,
        postUrl: String
    ): Flow<FetchState<Post?>> = channelFlow {
        callFetchFunc(service) {
            @Language("JS")
            val code = """
                const params = {
                    url: "$postUrl",
                };
                const feature = service.getFeature(AnyPostFeature);
                const fetchResult = feature.fetch(params);
                JSON.stringify(fetchResult)
            """.trimIndent()

            // Run js
            val text = evaluate(code, String::class.java)
            if (text == null) {
                send(FetchState.failure(Exception("Failed to load post")))
                return@callFetchFunc
            }

            // Parse json result
            val fetchResult = try {
                json.fromJson<JsFetchResult<JsPost>>(
                    json = text,
                    type = Json.parameterizedType<JsFetchResult<JsPost>>()
                )
            } catch (e: Exception) {
                Logger.e(TAG, "Illegal json: \n$text")
                e.printStackTrace()
                null
            }

            require(fetchResult != null) {
                "Cannot fetch this post: Illegal data returned from javascript, $postUrl"
            }

            if (fetchResult.isOk()) {
                val jsPost = fetchResult.data!!
                val post = jsPostToPost(service, jsPost)
                send(FetchState.success(value = post, isRemote = true))
            } else {
                val errorMessage = fetchResult.error ?: "No data passed from js"
                send(FetchState.failure(error = Exception(errorMessage)))
            }

            close()
        }
    }

    override fun searchPosts(
        service: ServiceManifest,
        query: String,
        pageKey: JsPageKey?
    ): Flow<PagedPostsFetchState> = channelFlow {
        callFetchFunc(service) {
            @Language("JS")
            val code = """
                const params = {
                    query: "$query",
                    pageKey: ${pageKey.toJsObject()},
                };
                const feature = service.getFeature(AnyPostFeature);
                const pagedResult = feature.search(params);
                JSON.stringify(pagedResult)
            """.trimIndent()

            // Run js
            val result = evaluate(code, String::class.java)
            if (result == null) {
                send(FetchState.failure(Exception("Failed to load posts")))
                return@callFetchFunc
            }

            send(convertJsPostPagedResult(service, result))

            close()
        }
    }

    override fun fetchComments(
        service: ServiceManifest,
        postUrl: String,
        commentsKey: String,
        pageKey: JsPageKey?,
    ): Flow<PagedCommentsFetchState> = channelFlow {
        callFetchFunc(service) {
            @Language("JS")
            val code = """
                const params = {
                    postUrl: "$postUrl",
                    loadKey: "$commentsKey",
                    pageKey: ${pageKey.toJsObject()},
                };
                const feature = service.getFeature(AnyPostFeature);
                const comments = feature.fetchComments(params);
                JSON.stringify(comments)
            """.trimIndent()

            val text = evaluate(code, String::class.java)
            if (text == null) {
                send(FetchState.failure(Exception("Failed to load comments")))
                return@callFetchFunc
            }

            val jsPagedResult = try {
                json.fromJson<JsPagedResult<Array<Comment>>>(
                    json = text,
                    type = Json.parameterizedType<JsPagedResult<Array<Comment>>>(),
                )!!
            } catch (e: Exception) {
                e.printStackTrace()
                Logger.e(TAG, "Illegal json: $text")
                null
            }

            require(jsPagedResult != null) {
                "Cannot fetch comments: Unsupported json returned from javascript"
            }

            if (jsPagedResult.isOk()) {
                val comments = jsPagedResult.data!!.toList()
                val prevKey = jsPagedResult.prevJsFetchKey()
                val nextKey = jsPagedResult.nextJsFetchKey()
                val result = PagedResult(
                    data = comments,
                    prevKey = prevKey,
                    nextKey = nextKey,
                )
                send(FetchState.success(value = result, isRemote = true))
            } else {
                val errorMessage = jsPagedResult.error ?: "No data passed from js"
                send(FetchState.failure(error = Exception(errorMessage)))
            }

            close()
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
            @Language("JS")
            val testCode = """
                const feature = service.getFeature(AnyPostFeature);
                const proto = Object.getPrototypeOf(feature);
                proto.hasOwnProperty("search")
            """.trimIndent()
            evaluate(testCode, Boolean::class.java) == true
        }
        emit(isSearchable.getOrDefault(false))
    }

    private fun convertJsFetchUserResult(
        service: ServiceManifest,
        result: String,
    ): FetchState<User> {
        // Parse json result
        val fetchResult = try {
            json.fromJson<JsFetchResult<JsUser>>(
                json = result,
                type = Json.parameterizedType<JsFetchResult<JsUser>>()
            )
        } catch (e: Exception) {
            Logger.e(TAG, "Illegal json: \n$result")
            e.printStackTrace()
            null
        }

        require(fetchResult != null) {
            "Cannot fetch this user: Illegal data returned from javascript"
        }

        return if (fetchResult.isOk()) {
            val jsUser = fetchResult.data!!
            val user = User.fromJsUser(serviceId = service.id, jsUser = jsUser)
            FetchState.success(value = user, isRemote = true)
        } else {
            val errorMessage = fetchResult.error ?: "Unknown error from js"
            FetchState.failure(error = Exception(errorMessage))
        }
    }

    private suspend fun convertJsPostPagedResult(
        service: ServiceManifest,
        result: String,
    ): PagedPostsFetchState {
        // Parse json result
        val jsPagedResult = try {
            json.fromJson<JsPagedResult<Array<JsPost>>>(
                json = result,
                type = Json.parameterizedType<JsPagedResult<Array<JsPost>>>(),
            )!!
        } catch (e: Exception) {
            e.printStackTrace()
            Logger.e(TAG, "Illegal json: $result")
            null
        }

        require(jsPagedResult != null) {
            "Cannot fetch posts: Unsupported json returned from javascript"
        }

        return if (jsPagedResult.isOk()) {
            val jsPosts = jsPagedResult.data!!
            val createdAt = System.currentTimeMillis() + jsPosts.size - 1
            val posts: List<Post> = jsPosts
                .distinctBy { it.url }
                .mapIndexed { index, post ->
                    jsPostToPost(service, post).copy(createdAt = createdAt - index)
                }
            val prevKey = jsPagedResult.prevJsFetchKey()
            val nextKey = jsPagedResult.nextJsFetchKey()
            val ret = PagedResult(
                data = posts,
                prevKey = prevKey,
                nextKey = nextKey,
            )
            FetchState.success(value = ret, isRemote = true)
        } else {
            val errorMessage = jsPagedResult.error ?: "No data passed from js"
            FetchState.failure(error = Exception(errorMessage))
        }
    }

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
        @OptIn(ExperimentalCoroutinesApi::class)
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
                commentCount = commentCount,
                commentsKey = commentsKey,
                openInBrowser = openInBrowser,
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