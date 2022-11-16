package any.data.repository

import android.content.Context
import any.data.db.AppDatabase
import any.data.db.BookmarkDao
import any.data.entity.Bookmark
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class BookmarkRepository(
    private val bookmarkDao: BookmarkDao
): ReactiveRepository<Unit, Bookmark>() {
    suspend fun getAll(): List<Bookmark> {
        return bookmarkDao.getAll()
    }

    fun getBookmarks(serviceId: String, postUrl: String): Flow<List<Bookmark>> = flow {
        emit(bookmarkDao.get(serviceId, postUrl))
    }

    fun findBookmark(
        serviceId: String,
        postUrl: String,
        elementIndex: Int
    ): Flow<Bookmark?> = flow {
        emit(bookmarkDao.find(serviceId, postUrl, elementIndex))
    }

    suspend fun addBookmark(bookmark: Bookmark) {
        bookmarkDao.add(bookmark)
        notifyInserted(bookmark)
    }

    suspend fun removePostBookmarks(serviceId: String, postUrl: String) {
        val bookmarks = bookmarkDao.get(serviceId, postUrl)
        if (bookmarks.isEmpty()) {
            return
        }
        bookmarkDao.remove(serviceId, postUrl)
        notifyDeletedByItem(bookmarks)
    }

    suspend fun removeBookmark(bookmark: Bookmark) {
        bookmarkDao.remove(bookmark)
        notifyDeletedByItem(bookmark)
    }

    suspend fun updateBookmark(bookmark: Bookmark) {
        bookmarkDao.update(bookmark)
        notifyUpdated(bookmark)
    }

    companion object {
        @Volatile
        private var instance: BookmarkRepository? = null

        fun getDefault(context: Context): BookmarkRepository {
            return instance ?: synchronized(BookmarkRepository::class) {
                instance ?: BookmarkRepository(
                    bookmarkDao = AppDatabase.get(context).bookmarkDao(),
                ).also { instance = it }
            }
        }
    }
}