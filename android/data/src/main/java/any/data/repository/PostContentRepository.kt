package any.data.repository

import android.content.Context
import any.data.db.PostContentDao
import any.data.db.PostContentDatabase
import any.data.entity.ContentElement
import any.data.entity.PostContent

class PostContentRepository(
    private val postContentDao: PostContentDao,
) : ReactiveRepository<String, PostContent>() {
    suspend fun size(): Int {
        return postContentDao.count()
    }

    suspend fun put(url: String, elements: List<ContentElement>?) {
        if (elements != null) {
            put(PostContent(url = url, elements = elements))
        }
    }

    suspend fun put(content: PostContent) {
        if (contains(content.url)) {
            postContentDao.update(content)
            notifyUpdated(content)
        } else {
            postContentDao.add(content)
            notifyInserted(content)
        }
    }

    suspend fun get(url: String): List<ContentElement>? {
        return postContentDao.get(url)?.elements
    }

    suspend fun keys(): Set<String> {
        return postContentDao.keys().toSet()
    }

    suspend fun contains(url: String): Boolean {
        return postContentDao.get(url) != null
    }

    suspend fun remove(url: String) {
        postContentDao.remove(url)
        notifyDeletedByKey(url)
    }

    suspend fun clear() {
        val urls = keys()
        postContentDao.clear()
        notifyDeletedByKey(urls.toList())
    }

    companion object {
        @Volatile
        private var instance: PostContentRepository? = null

        fun getDefault(context: Context): PostContentRepository {
            return instance ?: synchronized(PostContentRepository::class) {
                instance ?: PostContentRepository(
                    postContentDao = PostContentDatabase.get(context).postContentDao(),
                ).also {
                    instance = it
                }
            }
        }
    }
}