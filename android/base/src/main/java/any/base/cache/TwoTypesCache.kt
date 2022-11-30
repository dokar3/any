package any.base.cache

interface TwoTypesCache<K, IN, OUT> {
    fun contains(key: K): Boolean

    fun put(key: K, value: IN): OUT

    fun remove(key: K)

    fun get(key: K): OUT?

    fun clear()
}