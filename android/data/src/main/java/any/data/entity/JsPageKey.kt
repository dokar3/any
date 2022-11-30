package any.data.entity

import androidx.compose.runtime.Immutable
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
data class JsPageKey(
    val value: String?,
    val type: JsType,
)
