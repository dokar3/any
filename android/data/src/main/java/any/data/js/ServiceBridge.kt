package any.data.js

import any.data.FetchState
import any.data.entity.Comment
import any.data.entity.JsPageKey
import any.data.entity.PagedResult
import any.data.entity.Post
import any.data.entity.ServiceManifest
import any.data.entity.User
import kotlinx.coroutines.flow.Flow

typealias PagedPostsFetchState = FetchState<PagedResult<JsPageKey, List<Post>>>

typealias PagedCommentsFetchState = FetchState<PagedResult<JsPageKey, List<Comment>>>

interface ServiceBridge {
    fun fetchUserById(service: ServiceManifest, id: String): Flow<FetchState<User>>

    fun fetchUserByUrl(service: ServiceManifest, url: String): Flow<FetchState<User>>

    fun fetchUserPosts(
        service: ServiceManifest,
        userId: String,
        pageKey: JsPageKey?,
    ): Flow<PagedPostsFetchState>

    fun fetchLatestPosts(
        service: ServiceManifest,
        pageKey: JsPageKey?,
    ): Flow<PagedPostsFetchState>

    fun fetchPost(service: ServiceManifest, postUrl: String): Flow<FetchState<Post?>>

    fun isSearchable(service: ServiceManifest): Flow<Boolean>

    fun searchPosts(
        service: ServiceManifest,
        query: String,
        pageKey: JsPageKey?
    ): Flow<PagedPostsFetchState>

    fun fetchComments(
        service: ServiceManifest,
        postUrl: String,
        commentsKey: String,
        pageKey: JsPageKey?,
    ): Flow<PagedCommentsFetchState>
}