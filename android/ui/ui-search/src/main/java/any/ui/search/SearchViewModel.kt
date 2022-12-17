package any.ui.search

import android.content.Context
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import any.base.AndroidStrings
import any.base.Strings
import any.base.file.AndroidFileReader
import any.base.file.FileReader
import any.base.model.UiMessage
import any.base.util.messageForUser
import any.base.util.updateWith
import any.data.FetchState
import any.data.entity.JsPageKey
import any.data.entity.Post
import any.data.repository.PostRepository
import any.data.repository.ServiceRepository
import any.domain.entity.UiPost
import any.domain.entity.UiServiceManifest
import any.domain.post.toUiPosts
import any.domain.service.toUiManifest
import any.richtext.html.DefaultHtmlParser
import any.richtext.html.HtmlParser
import any.ui.common.BasePostViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class SearchViewModel(
    private val serviceRepository: ServiceRepository,
    postRepository: PostRepository,
    private val fileReader: FileReader,
    private val strings: Strings,
    htmlParser: HtmlParser = DefaultHtmlParser(),
    workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BasePostViewModel(
    postRepository = postRepository,
    htmlParser = htmlParser,
    workerDispatcher = workerDispatcher,
) {
    private val _searchUiState = MutableStateFlow(SearchUiState())
    val searchUiState: StateFlow<SearchUiState> = _searchUiState

    private var currPageKey: JsPageKey? = null
    private var nextPageKey: JsPageKey? = null

    private var currentSearchJob: Job? = null

    init {
        viewModelScope.launch {
            serviceRepository.changes.collect {
                selectService(_searchUiState.value.currentService?.id)
            }
        }
    }

    fun selectService(
        id: String?,
        onlyIfNoSelectedService: Boolean = true,
    ) {
        viewModelScope.launch(workerDispatcher) {
            val services = serviceRepository.getDbServices()
                .filter { it.isEnabled && it.areApiVersionsCompatible }
                .sortedBy { it.name }
                .map { it.toUiManifest(fileReader, htmlParser) }
            val currService = searchUiState.value.currentService
            val targetServiceId = if (onlyIfNoSelectedService && currService != null) {
                currService.id
            } else {
                id
            }
            val currLatest = services.find { it.id == targetServiceId } ?: services.firstOrNull()
            _searchUiState.update {
                if (targetServiceId != it.currentService?.id) {
                    it.copy(
                        services = services,
                        currentService = currLatest,
                        pageKey = null,
                        posts = emptyList(),
                        searchedCount = 0,
                    )
                } else {
                    it.copy(
                        services = services,
                        currentService = currLatest,
                        searchedCount = 0,
                    )
                }
            }
            if (currLatest != null) {
                val isSearchable = postRepository.isServiceSearchable(currLatest.raw)
                _searchUiState.update {
                    it.copy(isSearchable = isSearchable)
                }
            }
        }
    }

    fun selectService(service: UiServiceManifest) {
        _searchUiState.update {
            it.copy(
                currentService = service,
                pageKey = null,
                posts = emptyList(),
                searchedCount = 0,
            )
        }
        viewModelScope.launch(workerDispatcher) {
            val isSearchable = postRepository.isServiceSearchable(service.raw)
            _searchUiState.update {
                it.copy(isSearchable = isSearchable)
            }
        }
    }

    fun updateQuery(query: TextFieldValue) {
        _searchUiState.update {
            it.copy(query = query, hasSetQuery = true)
        }
    }

    fun refreshPostsFromCache() {
        val currService = _searchUiState.value.currentService ?: return
        val currPosts = _searchUiState.value.posts
        if (currPosts.isEmpty()) return
        viewModelScope.launch(workerDispatcher) {
            val latest = postRepository.updatePostsFromCache(
                service = currService.raw,
                posts = currPosts.map { it.raw },
            )
            _searchUiState.update {
                it.copy(posts = latest.toUiPosts(htmlParser))
            }
        }
    }

    fun search() {
        currentSearchJob?.cancel()
        _searchUiState.update {
            it.copy(
                pageKey = null,
                isLoading = true,
                isLoadingMore = false,
                posts = emptyList(),
                searchedCount = it.searchedCount + 1,
                message = null,
            )
        }
        search(key = null)
    }

    fun fetchNextPage() {
        if (nextPageKey != null) {
            _searchUiState.update {
                it.copy(
                    isLoading = true,
                    isLoadingMore = true,
                    message = null,
                )
            }
            search(key = nextPageKey)
        }
    }

    fun fetchPreviousRequest() {
        if (currPageKey == null) {
            search()
        } else {
            fetchNextPage()
        }
    }

    private fun search(key: JsPageKey?) {
        currPageKey = key
        currentSearchJob = viewModelScope.launch(workerDispatcher) {
            val uiState = _searchUiState.value
            val service = uiState.currentService ?: return@launch
            val query = uiState.query.text
            if (query.isEmpty()) {
                _searchUiState.update {
                    it.copy(
                        posts = emptyList(),
                        isLoading = false,
                        isLoadingMore = false,
                    )
                }
                return@launch
            }
            postRepository
                .searchRemotePosts(
                    service = service.raw,
                    query = query,
                    key = key,
                )
                .catch {
                    if (currentCoroutineContext().isActive) {
                        onSearchError(it)
                    }
                }
                .collect { state ->
                    if (!currentCoroutineContext().isActive) {
                        return@collect
                    }
                    when (state) {
                        is FetchState.Success -> {
                            val result = state.value
                            val currentPostUrls = _searchUiState.value.posts
                                .map { it.url }
                                .toHashSet()
                            val distinctPosts = result.data
                                .distinctBy { it.url }
                                .filter { !currentPostUrls.contains(it.url) }
                            onSearchSuccess(
                                posts = distinctPosts,
                                key = key,
                                nextKey = result.nextKey,
                            )
                        }

                        is FetchState.Failure -> {
                            onSearchError(state.error)
                        }

                        is FetchState.Loading -> {}
                        else -> {}
                    }
                }
        }.also {
            it.invokeOnCompletion {
                _searchUiState.update { state ->
                    state.copy(
                        isLoading = false,
                        isLoadingMore = false,
                    )
                }
            }
        }
    }

    private fun onSearchSuccess(
        posts: List<Post>,
        key: JsPageKey?,
        nextKey: JsPageKey?,
    ) {
        nextPageKey = nextKey
        val hasMore = posts.isNotEmpty() && nextKey != null
        val uiPosts = posts.toUiPosts(htmlParser)
        _searchUiState.update {
            it.copy(
                pageKey = key,
                posts = if (key == null) uiPosts else it.posts + uiPosts,
                isLoading = false,
                isLoadingMore = false,
                isSuccess = true,
                hasMore = hasMore,
            )
        }
    }

    private fun onSearchError(e: Throwable) {
        _searchUiState.update {
            it.copy(
                message = UiMessage.Error(e.messageForUser(strings)),
                isLoading = false,
                isLoadingMore = false,
                isSuccess = false,
            )
        }
    }

    fun clearMessage() {
        _searchUiState.update {
            it.copy(message = null)
        }
    }

    override suspend fun onPostsUpdated(posts: List<UiPost>) {
        val currPosts = _searchUiState.value.posts
        val updatedPosts = currPosts.updateWith(
            latest = posts,
            key = { it.url + it.serviceId },
        )
        if (currPosts != updatedPosts) {
            _searchUiState.update { it.copy(posts = updatedPosts) }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SearchViewModel(
                serviceRepository = ServiceRepository.getDefault(context),
                postRepository = PostRepository.getDefault(context),
                fileReader = AndroidFileReader(context),
                strings = AndroidStrings(context),
            ) as T
        }
    }
}