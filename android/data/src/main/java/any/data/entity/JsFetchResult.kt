package any.data.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class JsFetchResult<T>(
    val data: T?,
    val error: String?,
) {
    fun isOk(): Boolean {
        return data != null
    }

    fun isErr(): Boolean {
        return !isOk()
    }
}