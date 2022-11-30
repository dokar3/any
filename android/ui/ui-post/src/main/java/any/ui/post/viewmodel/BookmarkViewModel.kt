package any.ui.post.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import any.data.entity.Bookmark
import any.data.repository.BookmarkRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookmarkViewModel(
    private val bookmarkRepository: BookmarkRepository,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _bookmarkUiState = MutableStateFlow(BookmarkUiState())
    val bookmarkUiState: StateFlow<BookmarkUiState> = _bookmarkUiState

    fun loadBookmarks(serviceId: String, postUrl: String) {
        viewModelScope.launch(workerDispatcher) {
            bookmarkRepository.getBookmarks(serviceId, postUrl)
                .collect { bookmarks ->
                    _bookmarkUiState.update {
                        it.copy(bookmarks = bookmarks)
                    }
                }
        }
    }

    fun addBookmark(bookmark: Bookmark) {
        viewModelScope.launch(workerDispatcher) {
            bookmarkRepository.addBookmark(bookmark)
            loadBookmarks(bookmark.serviceId, bookmark.postUrl)
        }
    }

    fun removeBookmark(bookmark: Bookmark) {
        viewModelScope.launch(workerDispatcher) {
            bookmarkRepository.removeBookmark(bookmark)
            loadBookmarks(bookmark.serviceId, bookmark.postUrl)
        }
    }

    fun updateBookmark(bookmark: Bookmark) {
        viewModelScope.launch(workerDispatcher) {
            bookmarkRepository.updateBookmark(bookmark)
            loadBookmarks(bookmark.serviceId, bookmark.postUrl)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BookmarkViewModel(
                bookmarkRepository = BookmarkRepository.getDefault(context)
            ) as T
        }
    }
}