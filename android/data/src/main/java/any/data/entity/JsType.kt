package any.data.entity

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
enum class JsType {
    String,
    Number,
    Boolean,
    Null,
    Undefined,
}