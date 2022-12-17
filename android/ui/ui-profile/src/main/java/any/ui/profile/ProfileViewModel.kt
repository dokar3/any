package any.ui.profile

import any.base.R as BaseR
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import any.base.AndroidStrings
import any.base.Strings
import any.base.file.AndroidFileReader
import any.base.file.FileReader
import any.base.model.UiMessage
import any.base.util.messageForUser
import any.data.FetchState
import any.data.entity.JsPageKey
import any.data.entity.Post
import any.data.entity.User
import any.data.repository.FetchSources
import any.data.repository.PostRepository
import any.data.repository.ServiceRepository
import any.data.repository.UserRepository
import any.data.service.ServiceLookup
import any.domain.entity.UiPost
import any.domain.entity.UiServiceManifest
import any.domain.post.toUiPosts
import any.domain.service.toUiManifest
import any.domain.user.toUiUser
import any.richtext.html.DefaultHtmlParser
import any.richtext.html.HtmlParser
import any.ui.common.BasePostViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val serviceRepository: ServiceRepository,
    postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val strings: Strings,
    private val fileReader: FileReader,
    htmlParser: HtmlParser = DefaultHtmlParser(),
    workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BasePostViewModel(
    postRepository = postRepository,
    htmlParser = htmlParser,
    workerDispatcher = workerDispatcher,
) {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private var currPageKey: JsPageKey? = null
    private var nextPageKey: JsPageKey? = null

    private var fetchUserJob: Job? = null
    private var fetchPostsJob: Job? = null

    init {
        viewModelScope.launch(workerDispatcher) {
            userRepository.updates.collect { updates ->
                val currUser = uiState.value.user ?: return@collect
                val updatedUsers = updates.map { it.item }
                val updatedUser = updatedUsers.find {
                    it.serviceId == currUser.serviceId && it.id == currUser.id
                }
                if (updatedUser != null) {
                    _uiState.update {
                        it.copy(
                            user = updatedUser.toUiUser(
                                service = uiState.value.service?.raw,
                                htmlParser = htmlParser,
                            ),
                        )
                    }
                }
            }
        }
        viewModelScope.launch(workerDispatcher) {
            serviceRepository.updates.collect { updates ->
                val serviceId = uiState.value.service?.id ?: return@collect
                val updatedServices = updates.map { it.item }
                val updatedService = updatedServices.find { it.id == serviceId }
                if (updatedService != null) {
                    _uiState.update {
                        it.copy(
                            service = updatedService.toUiManifest(fileReader, htmlParser)
                        )
                    }
                }
            }
        }
    }

    fun fetchProfile(
        userUrl: String,
        remoteOnly: Boolean = false,
    ) {
        fetchUserJob?.cancel()
        fetchUserJob = viewModelScope.launch(workerDispatcher) {
            val services = serviceRepository.getDbServices()
            val service = ServiceLookup.find(services = services, userUrl = userUrl)
            if (service == null) {
                _uiState.update {
                    it.copy(
                        isLoadingUser = false,
                        message = UiMessage.Error(
                            strings(BaseR.string._no_service_found_for_url, userUrl)
                        ),
                    )
                }
                return@launch
            }
            val sources = if (remoteOnly) {
                FetchSources.remote().toOneShot()
            } else {
                FetchSources.all().toOneShot()
            }
            val flow = userRepository.fetchUserByUrl(
                service = service,
                userUrl = userUrl,
                sources = sources,
            )
            fetchProfile(
                service = service.toUiManifest(fileReader, htmlParser),
                flow = flow,
                postsFetchSources = sources,
            )
        }
    }

    fun fetchProfile(
        serviceId: String,
        userId: String,
        remoteOnly: Boolean = false,
    ) {
        fetchUserJob?.cancel()
        fetchUserJob = viewModelScope.launch(workerDispatcher) {
            val service = serviceRepository.findDbService(id = serviceId)
            if (service == null) {
                _uiState.update {
                    it.copy(
                        isLoadingUser = false,
                        message = UiMessage.Error(strings(BaseR.string.service_not_fond)),
                    )
                }
                return@launch
            }
            val sources = if (remoteOnly) {
                FetchSources.remote().toOneShot()
            } else {
                FetchSources.all().toOneShot()
            }
            val flow = userRepository.fetchUserById(
                service = service,
                userId = userId,
                sources = sources,
            )
            fetchProfile(
                service = service.toUiManifest(fileReader, htmlParser),
                flow = flow,
                postsFetchSources = sources,
            )
        }
    }

    private suspend fun fetchProfile(
        service: UiServiceManifest,
        flow: Flow<FetchState<User>>,
        postsFetchSources: FetchSources,
    ) {
        var postsFetchingStarted = false
        flow.onStart {
            _uiState.update {
                it.copy(
                    service = service,
                    isLoadingUser = true,
                    message = null,
                )
            }
        }.catch {
            onFetchUserError(it)
        }.collect { state ->
            when (state) {
                is FetchState.Failure -> {
                    onFetchUserError(state.error)
                }

                is FetchState.Success -> {
                    _uiState.update {
                        it.copy(
                            user = state.value.toUiUser(
                                service = service.raw,
                                htmlParser = htmlParser,
                            ),
                            isLoadingUser = false,
                        )
                    }
                    if (!postsFetchingStarted) {
                        postsFetchingStarted = true
                        fetchFirstPagePosts(sources = postsFetchSources)
                    }
                }

                is FetchState.Loading -> {}
            }
        }
    }

    private fun fetchFirstPagePosts(sources: FetchSources) {
        val service = uiState.value.service ?: return
        val user = uiState.value.user ?: return
        currPageKey = null
        fetchPostsJob?.cancel()
        fetchPostsJob = viewModelScope.launch(workerDispatcher) {
            postRepository.fetchInitialUserPosts(
                service = service.raw,
                user = user.raw,
                sources = sources,
            ).onStart {
                _uiState.update {
                    it.copy(
                        isLoadingPosts = true,
                        isFailedToFetchPosts = false,
                        message = null,
                    )
                }
            }.catch {
                onFetchPostsError(it)
            }.onCompletion {
                _uiState.update {
                    it.copy(isLoadingPosts = false)
                }
            }.collect { state ->
                when (state) {
                    is FetchState.Failure -> {
                        onFetchPostsError(state.error)
                    }

                    is FetchState.Success -> {
                        onFetchedFirstPagePosts(
                            posts = state.value.data,
                            nextKey = state.value.nextKey,
                            isRemote = state.isRemote,
                        )
                    }

                    is FetchState.Loading -> {}
                }
            }
        }
    }

    fun fetchMorePosts() {
        val pageKey = nextPageKey ?: return
        val service = uiState.value.service ?: return
        val user = uiState.value.user ?: return
        currPageKey = pageKey
        viewModelScope.launch(workerDispatcher) {
            postRepository.fetchMoreUserPosts(service.raw, user.raw, pageKey)
                .onStart {
                    _uiState.update {
                        it.copy(
                            isLoadingMorePosts = true,
                            isFailedToFetchPosts = false,
                            message = null,
                        )
                    }
                }
                .catch { onFetchPostsError(it) }
                .collect { state ->
                    when (state) {
                        is FetchState.Failure -> {
                            onFetchPostsError(state.error)
                        }

                        is FetchState.Success -> {
                            onFetchedMorePosts(
                                posts = state.value.data,
                                nextKey = state.value.nextKey,
                            )
                        }

                        is FetchState.Loading -> {}
                    }
                }
        }
    }

    fun retryPostsFetch() {
        viewModelScope.launch {
            if (currPageKey != null) {
                nextPageKey = currPageKey
                fetchMorePosts()
            } else {
                fetchFirstPagePosts(FetchSources.all())
            }
        }
    }

    private suspend fun onFetchedFirstPagePosts(
        posts: List<Post>,
        nextKey: JsPageKey?,
        isRemote: Boolean,
    ) {
        this.nextPageKey = nextKey
        val user = uiState.value.user
        if (isRemote && user != null) {
            val updated = user.raw.copy(pageKeyOfPage2 = nextKey)
            userRepository.update(updated)
        }
        _uiState.update {
            it.copy(
                posts = posts.distinctBy(Post::url).toUiPosts(htmlParser),
                hasMore = nextKey != null && posts.isNotEmpty(),
            )
        }
    }

    private fun onFetchedMorePosts(
        posts: List<Post>,
        nextKey: JsPageKey?,
    ) {
        this.nextPageKey = nextKey
        val combined = (uiState.value.posts + posts.toUiPosts(htmlParser))
            .distinctBy(UiPost::url)
        _uiState.update {
            it.copy(
                isLoadingMorePosts = false,
                posts = combined,
                hasMore = nextKey != null && posts.isNotEmpty(),
            )
        }
    }

    private fun onFetchUserError(error: Throwable) {
        _uiState.update {
            it.copy(
                isLoadingUser = false,
                message = UiMessage.Error(error.messageForUser(strings)),
            )
        }
    }

    private fun onFetchPostsError(error: Throwable) {
        error.printStackTrace()
        _uiState.update {
            it.copy(
                isLoadingPosts = false,
                isLoadingMorePosts = false,
                isFailedToFetchPosts = true,
                message = UiMessage.Error(error.messageForUser(strings)),
            )
        }
    }

    fun followUser() {
        val user = uiState.value.user ?: return
        viewModelScope.launch(workerDispatcher) {
            userRepository.update(user.raw.markFollowed())
        }
    }

    fun unfollowUser() {
        val user = uiState.value.user ?: return
        viewModelScope.launch(workerDispatcher) {
            userRepository.update(user.raw.markUnfollowed())
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override suspend fun onPostsUpdated(posts: List<UiPost>) {
        val currentPosts = uiState.value.posts.toMutableList()
        if (currentPosts.isEmpty()) {
            return
        }
        viewModelScope.launch(workerDispatcher) {
            for (post in posts) {
                val idx = currentPosts.indexOfFirst {
                    it.serviceId == post.serviceId && it.url == post.url
                }
                if (idx != -1) {
                    currentPosts[idx] = post
                }
            }
            _uiState.update { it.copy(posts = currentPosts) }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(
                serviceRepository = ServiceRepository.getDefault(context),
                postRepository = PostRepository.getDefault(context),
                userRepository = UserRepository.getDefault(context),
                strings = AndroidStrings(context),
                fileReader = AndroidFileReader(context),
            ) as T
        }
    }
}