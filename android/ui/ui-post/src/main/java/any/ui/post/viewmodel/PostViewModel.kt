package any.ui.post.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import any.base.AndroidStrings
import any.base.Strings
import any.base.UiMessage
import any.base.file.AndroidFileReader
import any.base.file.FileReader
import any.base.log.Logger
import any.base.util.messageForUser
import any.data.FetchState
import any.data.entity.Post
import any.data.entity.ServiceManifest
import any.data.js.ServiceApiVersion
import any.data.repository.PostContentRepository
import any.data.repository.PostRepository
import any.data.repository.PostRepository.FetchPostStrategy
import any.data.repository.ServiceRepository
import any.data.service.ServiceLookup
import any.domain.entity.UiPost
import any.domain.post.PostContentParser
import any.domain.post.toUiPost
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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PostViewModel(
    private val serviceRepository: ServiceRepository,
    private val postRepository: PostRepository,
    private val postContentRepository: PostContentRepository,
    private val fileReader: FileReader,
    private val strings: Strings,
    private val htmlParser: HtmlParser = DefaultHtmlParser(),
    private val postContentParser: PostContentParser = PostContentParser(htmlParser = htmlParser),
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _postUiState = MutableStateFlow(PostUiState())
    val postUiState: StateFlow<PostUiState> = _postUiState

    private var loadPostJob: Job? = null

    init {
        viewModelScope.launch {
            serviceRepository.updates.collect { updates ->
                val currServiceId = _postUiState.value.service?.id ?: return@collect
                val updatedServices = updates.map { it.item }
                val updatedService = updatedServices.find { it.id == currServiceId }
                if (updatedService != null) {
                    _postUiState.update {
                        it.copy(
                            service = updatedService.toUiManifest(fileReader, htmlParser)
                        )
                    }
                }
            }
        }
    }

    fun fetchPost(
        serviceId: String?,
        postUrl: String?,
        networkPostOnly: Boolean = false
    ) {
        loadPostJob?.cancel()
        loadPostJob = viewModelScope.launch(workerDispatcher) {
            if (postUrl == null) {
                return@launch
            }
            val service = findService(serviceId, postUrl)
            _postUiState.update {
                it.copy(
                    service = service?.toUiManifest(fileReader, htmlParser),
                    error = null,
                )
            }
            val strategy = if (networkPostOnly) {
                FetchPostStrategy.RemoteOnly
            } else {
                FetchPostStrategy.RemoteIfMissingContent
            }
            if (service != null) {
                if (!service.areApiVersionsCompatible) {
                    val requiredApiVersion = if (service.maxApiVersion != null) {
                        ">= ${service.minApiVersion}, <= ${service.maxApiVersion}"
                    } else {
                        ">= ${service.minApiVersion}"
                    }
                    val errorMessage = strings(
                        any.base.R.string._incompatible_service_required_api_versions,
                        requiredApiVersion,
                        ServiceApiVersion.get(),
                    )
                    _postUiState.update {
                        it.copy(error = UiMessage.Error(errorMessage))
                    }
                } else {
                    fetchPost(
                        service = service,
                        postServiceId = serviceId ?: service.id,
                        postUrl = postUrl,
                        strategy = strategy,
                    )
                }
            }
        }
    }

    private suspend fun fetchPost(
        service: ServiceManifest,
        postServiceId: String,
        postUrl: String,
        strategy: FetchPostStrategy,
    ) {
        postRepository.fetchPost(service, postServiceId, postUrl, strategy)
            .onStart {
                _postUiState.update {
                    it.copy(
                        isLoading = true,
                        loadingProgress = null,
                    )
                }
            }
            .onCompletion {
                _postUiState.update {
                    it.copy(isLoading = false)
                }
            }
            .catch { e ->
                if (currentCoroutineContext().isActive) {
                    onFetchPostError(error = e)
                }
            }
            .collect { state ->
                if (!currentCoroutineContext().isActive) {
                    return@collect
                }
                when (state) {
                    is FetchState.Success -> {
                        updatePost(state.value)
                    }

                    is FetchState.Failure -> {
                        onFetchPostError(error = state.error)
                    }

                    is FetchState.Loading -> {
                        val progressValue = state.progress.coerceIn(0f, 1f)
                        val progress = LoadingProgress(progressValue, state.message)
                        _postUiState.update { it.copy(loadingProgress = progress) }
                    }
                }
            }
    }

    private suspend fun updatePost(
        post: Post?,
        reversePages: Boolean = _postUiState.value.reversedPages,
    ) = withContext(workerDispatcher) {
        val currPost = post ?: _postUiState.value.post?.raw
        // Parse pics and sections
        val content = if (post != null) {
            val content = postContentRepository.get(post.url)
            if (content != null) {
                postContentParser.parse(content, reversePages)
            } else {
                PostContentParser.ParsedPostContent.Empty
            }
        } else {
            PostContentParser.ParsedPostContent.Empty
        }
        // Update ui state
        _postUiState.update {
            it.copy(
                post = currPost?.toUiPost(htmlParser),
                isCollected = currPost?.isCollected() ?: false,
                reversedPages = reversePages,
                contentElements = content.contentElements,
                images = content.images,
                sections = content.sections,
            )
        }
    }

    private fun onFetchPostError(error: Throwable) {
        error.printStackTrace()
        Logger.e(TAG, "loadFreshPost() failed: $error")
        _postUiState.update {
            it.copy(
                error = UiMessage.Error(error.messageForUser(strings)),
                isLoading = false,
            )
        }
    }

    private suspend fun findService(
        serviceId: String?,
        postUrl: String
    ): ServiceManifest? = withContext(workerDispatcher) {
        val services = serviceRepository.getDbServices()
        ServiceLookup.find(
            services = services,
            targetServiceId = serviceId,
            postUrl = postUrl,
        )
    }

    suspend fun canHandleUrlInApp(url: String): Boolean = withContext(workerDispatcher) {
        val service = findService(serviceId = null, postUrl = url)
        service != null && service.areApiVersionsCompatible
    }

    fun reversePages(reverse: Boolean) = viewModelScope.launch(workerDispatcher) {
        val post = _postUiState.value.post?.raw
        // Reversing
        updatePost(post, reverse)
    }

    fun savePost(
        serviceId: String,
        url: String,
        update: (Post) -> Post,
    ) = viewModelScope.launch(workerDispatcher) {
        // Update db only, no need to change ui state
        val latest = postRepository.loadCachedPost(serviceId, url) ?: return@launch
        if (latest.isInUsing()) {
            val updated = update(latest)
            postRepository.updatePost(updated)
        }
    }

    fun collectPost(post: UiPost) = viewModelScope.launch(workerDispatcher) {
        postRepository.collectPost(post.raw)
        reloadPost(post.raw)
    }

    fun discardPost(post: UiPost) = viewModelScope.launch(workerDispatcher) {
        postRepository.discardPost(post.raw)
        reloadPost(post.raw)
    }

    fun removePostContentCache(post: UiPost) = viewModelScope.launch(workerDispatcher) {
        postContentRepository.remove(post.url)
    }

    fun clearError() {
        _postUiState.update {
            it.copy(error = null)
        }
    }

    private suspend fun reloadPost(post: Post) {
        val cachedPost = postRepository.loadCachedPost(post.serviceId, post.url)
        if (cachedPost != null) {
            _postUiState.update {
                it.copy(
                    post = cachedPost.toUiPost(htmlParser),
                    isCollected = cachedPost.isCollected(),
                )
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val postContentStore = PostContentRepository.getDefault(context)
            return PostViewModel(
                serviceRepository = ServiceRepository.getDefault(context),
                postRepository = PostRepository.getDefault(context),
                postContentRepository = postContentStore,
                fileReader = AndroidFileReader(context),
                strings = AndroidStrings(context),
            ) as T
        }
    }

    companion object {
        private const val TAG = "PostContentVM"
    }
}