package any.data.testing.dao

import any.data.db.PostContentDao
import any.data.entity.PostContent

class TestPostContentDao : PostContentDao {
    private val cache = mutableMapOf<String, PostContent>()

    override suspend fun get(url: String): PostContent? {
        return cache[url]
    }

    override suspend fun getAll(): List<PostContent> {
        return cache.values.toList()
    }

    override suspend fun add(content: PostContent) {
        cache[content.url] = content
    }

    override suspend fun add(contents: List<PostContent>) {
        for (content in contents) {
            add(content)
        }
    }

    override suspend fun remove(content: PostContent) {
        cache.remove(content.url)
    }

    override suspend fun remove(url: String) {
        cache.remove(url)
    }

    override suspend fun update(content: PostContent) {
        cache[content.url] = content
    }

    override suspend fun keys(): List<String> {
        return cache.keys.toList()
    }

    override suspend fun count(): Int {
        return cache.size
    }

    override suspend fun clear() {
        cache.clear()
    }
}