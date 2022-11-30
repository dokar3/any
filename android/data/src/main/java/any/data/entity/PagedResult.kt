package any.data.entity

data class PagedResult<K, T>(
    val data: T,
    val prevKey: K? = null,
    val nextKey: K? = null,
)
