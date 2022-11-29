package any.base.cache

interface Cache<K, V> {
    fun contains(key: K): Boolean

    fun put(key: K, value: V)

    fun remove(key: K)

    operator fun get(key: K): V?

    fun getAll(): List<V>

    fun clear()

    operator fun set(key: K, value: V) {
        put(key, value)
    }
}