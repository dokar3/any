package any.data.testing.source.post

import any.data.FetchState
import any.data.entity.Comment
import any.data.entity.JsPageKey
import any.data.entity.JsType
import any.data.entity.PagedResult
import any.data.entity.Post
import any.data.entity.ServiceManifest
import any.data.entity.User
import any.data.js.ServiceBridge
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TestServiceBridge : ServiceBridge {
    private val allPosts: MutableMap<Int, List<Post>> = mutableMapOf()

    fun setPosts(page: Int, posts: List<Post>) {
        allPosts[page] = posts
    }

    override fun isSearchable(service: ServiceManifest): Flow<Boolean> = flow {
        emit(false)
    }

    override fun fetchComments(
        service: ServiceManifest,
        postUrl: String,
        commentsKey: String,
        pageKey: JsPageKey?
    ): Flow<FetchState<PagedResult<JsPageKey, List<Comment>>>> = flow {
        emit(FetchState.success(value = PagedResult(data = emptyList()), isRemote = true))
    }

    override fun searchPosts(
        service: ServiceManifest,
        query: String,
        pageKey: JsPageKey?
    ): Flow<FetchState<PagedResult<JsPageKey, List<Post>>>> = flow {
        emit(FetchState.success(value = PagedResult(data = emptyList()), isRemote = true))
    }

    override fun fetchUserById(service: ServiceManifest, id: String): Flow<FetchState<User>> {
        TODO("Not yet implemented")
    }

    override fun fetchUserByUrl(service: ServiceManifest, url: String): Flow<FetchState<User>> {
        TODO("Not yet implemented")
    }

    override fun fetchUserPosts(
        service: ServiceManifest,
        userId: String,
        pageKey: JsPageKey?
    ): Flow<FetchState<PagedResult<JsPageKey, List<Post>>>> {
        TODO("Not yet implemented")
    }

    override fun fetchLatestPosts(
        service: ServiceManifest,
        pageKey: JsPageKey?
    ): Flow<FetchState<PagedResult<JsPageKey, List<Post>>>> = flow {
        val result = if (pageKey != null) {
            val value = pageKey.value
            require(value != null) { "Page key value cannot be null" }
            require(pageKey.type == JsType.Number) {
                "Only number page key is supported in testing"
            }
            val page = value.toInt()
            val posts = allPosts[page] ?: return@flow
            PagedResult(
                data = posts.filter { it.serviceId == service.id },
                nextKey = JsPageKey(
                    value = (page + 1).toString(),
                    type = JsType.Number,
                )
            )
        } else {
            val posts = allPosts[1] ?: return@flow
            PagedResult(data = posts.filter { it.serviceId == service.id })
        }
        emit(FetchState.success(value = result, isRemote = true))
    }

    override fun fetchPost(service: ServiceManifest, postUrl: String): Flow<FetchState<Post?>> =
        flow {
            val post = allPosts.values
                .flatten()
                .find { it.serviceId == service.id && it.url == postUrl }
            emit(FetchState.success(value = post, isRemote = true))
        }
}