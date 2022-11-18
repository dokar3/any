package any.ui.comments.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import any.base.UiMessage
import any.base.file.AndroidFileReader
import any.base.file.FileReader
import any.base.log.Logger
import any.data.FetchState
import any.data.ServiceLookup
import any.data.entity.Comment
import any.data.entity.JsPageKey
import any.data.entity.Post
import any.data.repository.PostRepository
import any.data.repository.ServiceRepository
import any.domain.entity.UiServiceManifest
import any.domain.service.toUiManifest
import any.richtext.html.DefaultHtmlParser
import any.richtext.html.HtmlParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentsViewModel(
    private val serviceRepository: ServiceRepository,
    private val postRepository: PostRepository,
    private val fileReader: FileReader,
    private val htmlParser: HtmlParser,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState

    private var fetchCommentsJob: Job? = null

    private var currentService: UiServiceManifest? = null
    private var currentPost: Post? = null
    private var nextPageKey: JsPageKey? = null

    private var allComments: List<UiComment>? = null

    fun resetIfNeeded(service: UiServiceManifest?, post: Post) {
        if (currentService != service || currentPost != post) {
            _uiState.update { CommentsUiState(isLoading = true) }
        }
    }

    fun fetchFirstPage(service: UiServiceManifest?, post: Post) {
        fetchComments(service, post, null)
    }

    fun fetchMore(service: UiServiceManifest?, post: Post) {
        if (nextPageKey != null) {
            fetchComments(service, post, nextPageKey)
        }
    }

    fun fetchPrevRequest() {
        val service = currentService ?: return
        val post = currentPost ?: return
        if (nextPageKey == null) {
            fetchFirstPage(service, post)
        } else {
            fetchMore(service, post)
        }
    }

    private fun fetchComments(
        service: UiServiceManifest?,
        post: Post,
        pageKey: JsPageKey?,
    ) {
        currentPost = post
        fetchCommentsJob?.cancel()
        fetchCommentsJob = viewModelScope.launch(Dispatchers.Default) {
            val targetService = service ?: findService(post) ?: return@launch
            currentService = targetService
            val commentsKey = post.commentsKey ?: return@launch
            postRepository.fetchComments(
                service = targetService.raw,
                postUrl = post.url,
                commentsKey = commentsKey,
                pageKey = pageKey,
            )
                .onStart {
                    _uiState.update {
                        it.copy(
                            isLoading = true,
                            isLoadingMore = pageKey != null,
                            isSuccess = false,
                            message = null,
                        )
                    }
                }
                .catch {
                    if (currentCoroutineContext().isActive) {
                        onFetchCommentsFailed(it)
                    }
                }
                .collect { state ->
                    if (!currentCoroutineContext().isActive) {
                        return@collect
                    }
                    when (state) {
                        is FetchState.Success -> {
                            val result = state.value
                            onFetchCommentsSuccess(
                                comments = result.data,
                                pageKey = pageKey,
                                nextKey = result.nextKey,
                            )
                        }

                        is FetchState.Failure -> {
                            onFetchCommentsFailed(state.error)
                        }

                        else -> {}
                    }
                }
        }.also {
            it.invokeOnCompletion {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isLoadingMore = false,
                    )
                }
            }
        }
    }

    private suspend fun findService(
        post: Post
    ): UiServiceManifest? = withContext(workerDispatcher) {
        val services = serviceRepository.getDbServices()
        val service = ServiceLookup.find(
            services = services,
            targetServiceId = post.serviceId,
            postUrl = post.url,
        )
        service?.toUiManifest(fileReader, htmlParser)
    }

    private fun onFetchCommentsSuccess(
        comments: List<Comment>,
        pageKey: JsPageKey?,
        nextKey: JsPageKey?,
    ) {
        nextPageKey = nextKey
        val uiComments = comments.toUiComments()
        val collapsed = uiComments.collapseReplies(expandCount = INITIAL_EXPANDED_REPLY_COUNT)
        val combinedComments = if (pageKey != null) {
            allComments = (allComments ?: emptyList()) + uiComments
            _uiState.value.comments + collapsed
        } else {
            allComments = uiComments
            collapsed
        }
        val hasMore = comments.isNotEmpty() && nextKey != null
        _uiState.update {
            it.copy(
                pageKey = pageKey,
                comments = combinedComments,
                hasMore = hasMore,
                isLoading = false,
                isLoadingMore = false,
                isSuccess = true,
            )
        }
    }

    private fun onFetchCommentsFailed(e: Throwable) {
        Logger.e(TAG, "fetchComments() error: $e")
        _uiState.update {
            it.copy(
                isLoading = false,
                isLoadingMore = false,
                isSuccess = false,
                message = UiMessage.Error(e.message ?: "Unknown error")
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun expandReplies(
        item: UiComment.ExpandReplies,
    ) = viewModelScope.launch(workerDispatcher) {
        val all = allComments
        if (all.isNullOrEmpty()) {
            return@launch
        }

        val comments = uiState.value.comments

        val expandedEnd = comments.indexOfFirst { it == item }
        if (expandedEnd == -1) {
            return@launch
        }

        val commentIndex = comments.indexOfFirst {
            it is UiComment.Comment && it.id == item.commentId
        }
        if (commentIndex == -1 || commentIndex == comments.lastIndex) {
            return@launch
        }
        val comment = comments[commentIndex] as UiComment.Comment
        val replies = all.getReplies(target = comment)

        val newList = comments.subList(0, commentIndex + 1).toMutableList()
        // Add expanded replies
        newList.addAll(replies)
        // Add the collapse item
        newList.add(
            UiComment.CollapseReplies(
                commentId = item.commentId,
                count = item.count,
            )
        )
        // Add rest comments
        if (expandedEnd != comments.lastIndex) {
            newList.addAll(comments.subList(expandedEnd + 1, comments.size))
        }

        // Update
        _uiState.update { it.copy(comments = newList) }
    }

    fun collapseReplies(
        item: UiComment.CollapseReplies,
    ) = viewModelScope.launch(workerDispatcher) {
        val comments = uiState.value.comments

        val end = comments.indexOfFirst { it == item }
        if (end == -1) {
            return@launch
        }

        val commentIndex = comments.indexOfFirst {
            it is UiComment.Comment && it.id == item.commentId
        }
        if (commentIndex == -1 || commentIndex == comments.lastIndex) {
            return@launch
        }

        val newList = comments.subList(0, commentIndex).toMutableList()

        val toCollapse = comments.subList(commentIndex, end)
        val collapsed = toCollapse.collapseReplies(expandCount = INITIAL_EXPANDED_REPLY_COUNT)
        // Add collapsed replies
        newList.addAll(collapsed)
        if (end != comments.lastIndex) {
            // Add rest comments
            newList.addAll(comments.subList(end + 1, comments.size))
        }

        // Update
        _uiState.update { it.copy(comments = newList) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CommentsViewModel(
                serviceRepository = ServiceRepository.getDefault(context),
                postRepository = PostRepository.getDefault(context),
                fileReader = AndroidFileReader(context),
                htmlParser = DefaultHtmlParser(),
            ) as T
        }
    }

    companion object {
        private const val TAG = "CommentsViewModel"

        private const val INITIAL_EXPANDED_REPLY_COUNT = 3
    }
}