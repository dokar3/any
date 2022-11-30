package any.base.cache

import androidx.collection.LruCache

/**
 * A wrapper of the [LruCache]. The default value of `maxSize` is 1000.
 */
open class MemoryCache<K : Any, V : Any>(
    maxSize: Int = 1000
) {
    protected val cache = LruCache<K, V>(maxSize)

    fun contains(key: K): Boolean {
        return cache.get(key) != null
    }

    fun put(key: K, value: V) {
        cache.put(key, value)
    }

    operator fun set(key: K, value: V) {
        put(key, value)
    }

    fun remove(key: K) {
        cache.remove(key)
    }

    operator fun get(key: K): V? {
        return cache[key]
    }

    fun keys(): Set<K> {
        return cache.snapshot().keys
    }

    fun values(): List<V> {
        return cache.snapshot().values.toList()
    }

    fun entities(): Map<K, V> {
        return cache.snapshot()
    }

    fun clear() {
        return cache.evictAll()
    }
}