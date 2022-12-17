package any.ui.readingbubble

import any.ui.readingbubble.entity.ReadingPost
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ReadingBubbleViewModel(
    private val viewModelScope: CoroutineScope,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val _uiState = MutableStateFlow(ReadingBubbleUiState())
    val uiState: StateFlow<ReadingBubbleUiState> = _uiState

    fun addPost(post: ReadingPost) {
        viewModelScope.launch(workerDispatcher) {
            val p = _uiState.value.posts.indexOfFirst {
                it.serviceId == post.serviceId && it.url == post.url
            }
            if (p != -1) return@launch
            _uiState.update {
                it.copy(posts = it.posts + post)
            }
        }
    }

    fun removePost(post: ReadingPost) {
        viewModelScope.launch(workerDispatcher) {
            val posts = _uiState.value.posts.toMutableList()
            val idx = posts.indexOfFirst {
                it.serviceId == post.serviceId && it.url == post.url
            }
            if (idx == -1) return@launch
            posts.removeAt(idx)
            _uiState.update {
                it.copy(posts = posts.toList())
            }
        }
    }

    fun hasPost(post: ReadingPost): Boolean {
        return _uiState.value.posts.indexOfFirst {
            it.serviceId == post.serviceId && it.url == post.url
        } != -1
    }

    fun clearPosts() {
        _uiState.update {
            it.copy(posts = emptyList())
        }
    }
}