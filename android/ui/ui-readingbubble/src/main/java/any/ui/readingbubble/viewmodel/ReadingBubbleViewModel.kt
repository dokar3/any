package any.ui.readingbubble.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import any.ui.readingbubble.entity.ReadingPost
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReadingBubbleViewModel(
    private val bgDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReadingBubbleUiState())
    val uiState: StateFlow<ReadingBubbleUiState> = _uiState

    fun addPost(post: ReadingPost) {
        viewModelScope.launch(bgDispatcher) {
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
        viewModelScope.launch(bgDispatcher) {
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