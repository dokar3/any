package any.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import any.data.repository.PostRepository
import any.domain.entity.UiPost
import any.domain.post.toUiPost
import any.domain.post.toUiPosts
import any.richtext.html.HtmlParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

abstract class BasePostViewModel(
    protected val postRepository: PostRepository,
    protected val htmlParser: HtmlParser,
    protected val workerDispatcher: CoroutineDispatcher,
) : ViewModel() {
    init {
        viewModelScope.launch {
            postRepository.postsUpdateFlow()
                .map { it.toUiPosts(htmlParser) }
                .collect {
                    if (it.size == 1) {
                        onPostsUpdated(it)
                    }
                }
        }
    }

    fun updatePost(post: UiPost) {
        viewModelScope.launch(workerDispatcher) {
            postRepository.updatePost(post.raw)
            reloadPost(post)
        }
    }

    fun collectPost(post: UiPost) {
        viewModelScope.launch(workerDispatcher) {
            postRepository.collectPost(post.raw)
            reloadPost(post)
        }
    }

    fun discardPost(post: UiPost) {
        viewModelScope.launch(workerDispatcher) {
            postRepository.discardPost(post.raw)
            reloadPost(post)
        }
    }

    fun addToFolder(post: UiPost, folderName: String) {
        val toUpdate = post.copy(
            folder = folderName,
            collectedAt = System.currentTimeMillis(),
        )
        updatePost(toUpdate)
    }

    private suspend fun reloadPost(post: UiPost) {
        val cachedPost = postRepository.loadCachedPost(post.serviceId, post.url)
            ?.toUiPost(htmlParser)
        if (cachedPost != null) {
            onPostsUpdated(listOf(cachedPost))
        }
    }

    abstract suspend fun onPostsUpdated(posts: List<UiPost>)
}