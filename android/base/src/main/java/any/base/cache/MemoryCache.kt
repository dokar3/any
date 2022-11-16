package any.base.cache

import androidx.collection.LruCache

open class MemoryCache<K : Any, V : Any>(
    maxSize: Int = 1000
) : Cache<K, V> {
    protected val cache = LruCache<K, V>(maxSize)

    override fun contains(key: K): Boolean {
        return cache.get(key) != null
    }

    override fun put(key: K, value: V) {
        cache.put(key, value)
    }

    override fun remove(key: K) {
        cache.remove(key)
    }

    override fun get(key: K): V? {
        return cache[key]
    }

    override fun getAll(): List<V> {
        return cache.snapshot().values.toList()
    }

    override fun clear() {
        return cache.evictAll()
    }
}