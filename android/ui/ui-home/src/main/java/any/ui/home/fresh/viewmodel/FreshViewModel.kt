package any.ui.home.fresh.viewmodel

import any.base.R as BaseR
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import any.base.AndroidStrings
import any.base.Strings
import any.base.file.AndroidFileReader
import any.base.file.FileReader
import any.base.image.ImageRequest
import any.base.model.UiMessage
import any.base.prefs.PreferencesStore
import any.base.prefs.currentService
import any.base.prefs.preferencesStore
import any.base.util.messageForUser
import any.base.util.updateWith
import any.data.Comparators
import any.data.FetchState
import any.data.entity.JsPageKey
import any.data.entity.Post
import any.data.entity.ServiceManifest
import any.data.repository.PostRepository
import any.data.repository.PostRepository.FetchLatestStrategy
import any.data.repository.ServiceRepository
import any.domain.entity.UiPost
import any.domain.entity.UiServiceManifest
import any.domain.post.PostMediaImageCollector
import any.domain.post.toUiPosts
import any.domain.service.toUiManifest
import any.richtext.html.DefaultHtmlParser
import any.richtext.html.HtmlParser
import any.ui.common.viewmodel.BasePostViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FreshViewModel(
    private val serviceRepository: ServiceRepository,
    postRepository: PostRepository,
    private val preferencesStore: PreferencesStore,
    private val strings: Strings,
    private val fileReader: FileReader,
    htmlParser: HtmlParser = DefaultHtmlParser(),
    workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BasePostViewModel(
    postRepository = postRepository,
    htmlParser = htmlParser,
    workerDispatcher = workerDispatcher,
) {
    private val _freshUiState = MutableStateFlow(FreshUiState())
    val freshUiState: StateFlow<FreshUiState> = _freshUiState

    private val _currService: UiServiceManifest?
        get() = _freshUiState.value.currService

    private var currPageKey: JsPageKey? = null
    private var nextPageKey: JsPageKey? = null

    private val _currPosts: List<UiPost>
        get() = _freshUiState.value.posts

    private var fetchPostsJob: Job? = null

    init {
        viewModelScope.launch(workerDispatcher) {
            // Listen service list updates
            serviceRepository.changes.collect { loadServices() }
        }
        viewModelScope.launch(workerDispatcher) {
            // Listen current service updates
            _freshUiState.mapNotNull { it.currService }
                .distinctUntilChangedBy { it.id }
                .collect { currService -> fetchInitialPosts(currService) }
        }
    }

    fun loadServices() {
        viewModelScope.launch(workerDispatcher) {
            val services = serviceRepository.getDbServices()
                .filter { it.isEnabled && it.areApiVersionsCompatible }
                .sortedWith(Comparators.serviceManifestNameComparator)
                .map { it.toUiManifest(fileReader, htmlParser) }
            val currServiceId = preferencesStore.currentService.value
            val currService = services.find { it.id == currServiceId } ?: services.firstOrNull()
            _freshUiState.update { it.copy(services = services, currService = currService) }
            if (currService == null) {
                cancelLoadingsAndClearInvalidCache()
                _freshUiState.update {
                    it.copy(isLoadingInitialPosts = false, posts = emptyList())
                }
            }
        }
    }

    /**
     * Set the current service.
     */
    fun setCurrentService(service: UiServiceManifest) {
        val isSwitched = _currService?.id != service.id
        if (isSwitched) {
            _freshUiState.update {
                it.copy(
                    posts = emptyList(),
                    isLoadingInitialPosts = true,
                )
            }
        }
        cancelLoadingsAndClearInvalidCache()
        nextPageKey = null
        preferencesStore.currentService.value = service.id
        _freshUiState.update { it.copy(currService = service, pageKey = null) }
    }

    /**
     * Fetch the initial posts
     */
    fun fetchInitialPosts(service: UiServiceManifest, remoteOnly: Boolean = false) {
        currPageKey = null
        val strategy = if (remoteOnly) {
            FetchLatestStrategy.RemoteOnly
        } else {
            FetchLatestStrategy.RemoteIfEmptyCache
        }
        if (remoteOnly) {
            _freshUiState.update {
                it.copy(requireRefreshInitialPage = false)
            }
        }
        _freshUiState.update {
            it.copy(message = null, isLoadingInitialPosts = true)
        }
        fetchPostsJob?.cancel()
        fetchPostsJob = viewModelScope.launch(workerDispatcher) {
            postRepository.fetchInitialPosts(service.raw, strategy)
                .catch {
                    if (currentCoroutineContext().isActive) {
                        onError(it)
                    }
                }
                .collect { state ->
                    if (!currentCoroutineContext().isActive) {
                        return@collect
                    }
                    when (state) {
                        is FetchState.Success -> {
                            val result = state.value
                            val distinctPosts = result.data.distinctBy { it.url }
                            onInitialPostsLoaded(
                                service = service.raw,
                                posts = distinctPosts,
                                isRemote = state.isRemote,
                                nextKey = result.nextKey,
                            )
                        }

                        is FetchState.Failure -> {
                            onError(state.error)
                        }

                        is FetchState.Loading -> {}
                    }
                }
        }.also {
            it.invokeOnCompletion { onFetchComplete() }
        }
    }

    /**
     * Fetch the next page
     */
    fun fetchMorePosts(service: UiServiceManifest) {
        val pageKey = nextPageKey
        currPageKey = nextPageKey
        if (pageKey == null) {
            _freshUiState.update {
                it.copy(requireRefreshInitialPage = true)
            }
            return
        }
        _freshUiState.update {
            it.copy(message = null, isLoadingMorePosts = true)
        }
        fetchPostsJob?.cancel()
        fetchPostsJob = viewModelScope.launch(workerDispatcher) {
            postRepository.fetchMorePosts(service.raw, pageKey)
                .catch {
                    if (currentCoroutineContext().isActive) {
                        onError(it)
                    }
                }
                .collect { state ->
                    if (!currentCoroutineContext().isActive) {
                        return@collect
                    }
                    when (state) {
                        is FetchState.Success -> {
                            val result = state.value
                            val currentPostUrls = _currPosts.map { it.url }.toHashSet()
                            val distinctPosts = result.data
                                .distinctBy { it.url }
                                .filter { !currentPostUrls.contains(it.url) }
                            onMorePostsLoaded(
                                posts = distinctPosts,
                                key = pageKey,
                                nextKey = result.nextKey,
                            )
                        }

                        is FetchState.Failure -> {
                            onError(state.error)
                        }

                        is FetchState.Loading -> {}
                    }
                }
        }.also {
            it.invokeOnCompletion { onFetchComplete() }
        }
    }

    /**
     * Fetch the previous fetch request, used to retry after failed.
     */
    fun fetchPreviousRequest(service: UiServiceManifest, remoteOnly: Boolean) {
        if (currPageKey == null) {
            fetchInitialPosts(service, remoteOnly)
        } else {
            fetchMorePosts(service)
        }
    }

    private fun onFetchComplete() {
        _freshUiState.update { state ->
            state.copy(
                isLoadingInitialPosts = false,
                isLoadingMorePosts = false,
            )
        }
    }

    private suspend fun onInitialPostsLoaded(
        service: ServiceManifest,
        posts: List<Post>,
        isRemote: Boolean,
        nextKey: JsPageKey?,
    ) {
        nextPageKey = nextKey

        // Update the page key of page 2, so we can directly fetch page 2 next time
        // without fetching the initial page to get the page key
        val latestService = serviceRepository.findDbService(service.id) ?: service
        serviceRepository.updateDbService(latestService.copy(pageKeyOfPage2 = nextKey))

        val postImages = posts.map(PostMediaImageCollector::collect)
            .flatten()
            .map(ImageRequest::Url)

        val message = if (isRemote) {
            val currPostUrls = _currPosts.map(UiPost::url).toHashSet()
            val newPostCount = posts.count {
                !currPostUrls.contains(it.url)
            }
            val messageText = if (newPostCount == 0) {
                strings(BaseR.string.no_new_posts)
            } else {
                strings(BaseR.string._loaded_new_posts, newPostCount)
            }
            UiMessage.Normal(messageText)
        } else {
            _freshUiState.value.message
        }

        _freshUiState.update {
            it.copy(
                isLoadingInitialPosts = false,
                isSuccess = true,
                hasMore = true,
                pageKey = null,
                message = message,
                posts = posts.toUiPosts(htmlParser),
                allPostMediaImages = postImages,
            )
        }
    }

    private fun onMorePostsLoaded(
        posts: List<Post>,
        key: JsPageKey?,
        nextKey: JsPageKey?,
    ) {
        val uiPosts = posts.toUiPosts(htmlParser)
        val combinedPosts: List<UiPost> = if (uiPosts.isNotEmpty()) {
            _currPosts + uiPosts
        } else {
            _currPosts
        }
        val hasMore = posts.isNotEmpty() && nextKey != null
        nextPageKey = if (hasMore) nextKey else null
        _freshUiState.update {
            it.copy(
                isLoadingMorePosts = false,
                isSuccess = true,
                hasMore = hasMore,
                posts = combinedPosts,
                pageKey = key,
            )
        }
    }

    private fun onError(e: Throwable) {
        e.printStackTrace()
        val message = UiMessage.Error(e.messageForUser(strings))
        _freshUiState.update {
            it.copy(
                message = message,
                isLoadingInitialPosts = false,
                isLoadingMorePosts = false,
                isSuccess = false,
            )
        }
    }

    /**
     * Clear the ui message
     */
    fun clearMessage() {
        _freshUiState.update {
            it.copy(message = null)
        }
    }

    /**
     * Cancel loading jobs
     */
    fun cancelLoadings() {
        fetchPostsJob?.cancel()
    }

    /**
     * Clear cached fresh posts
     */
    fun clearFreshPosts() {
        val service = _currService
        if (service != null) {
            viewModelScope.launch(workerDispatcher) {
                postRepository.clearFresh(service.raw)
            }
        }
    }

    private fun cancelLoadingsAndClearInvalidCache() {
        fetchPostsJob?.cancel()
        _currService?.let { service ->
            viewModelScope.launch {
                postRepository.clearUnused(service.raw)
            }
        }
    }

    override suspend fun onPostsUpdated(posts: List<UiPost>) {
        val currPosts = _freshUiState.value.posts
        val updatedPosts = currPosts.updateWith(
            latest = posts,
            key = { it.url + it.serviceId },
        )
        if (currPosts != updatedPosts) {
            _freshUiState.update { it.copy(posts = updatedPosts) }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FreshViewModel(
                serviceRepository = ServiceRepository.getDefault(context),
                postRepository = PostRepository.getDefault(context),
                preferencesStore = context.preferencesStore(),
                strings = AndroidStrings(context),
                fileReader = AndroidFileReader(context),
            ) as T
        }
    }
}
