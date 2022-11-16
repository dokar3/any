package any.data.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsPagedResult<T>(
    val data: T?,
    val prevKey: Any?,
    val nextKey: Any?,
    val error: String?,
) {
    fun isOk(): Boolean {
        return data != null
    }

    fun isErr(): Boolean {
        return !isOk()
    }

    fun prevJsFetchKey(): JsPageKey? {
        return keyToJsFetchKey(prevKey)
    }

    fun nextJsFetchKey(): JsPageKey? {
        return keyToJsFetchKey(nextKey)
    }

    private fun keyToJsFetchKey(key: Any?): JsPageKey? {
        if (key == null) {
            return null
        }
        return when (key) {
            is String -> JsPageKey(value = key, type = JsType.String)
            is Number -> JsPageKey(value = key.toString(), type = JsType.Number)
            else -> throw IllegalArgumentException(
                "Unsupported fetch key type: ${key::class.java}"
            )
        }
    }
}