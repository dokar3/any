package any.data.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import com.squareup.moshi.JsonClass

@Immutable
@JsonClass(generateAdapter = true)
@Entity(primaryKeys = ["serviceId", "postUrl", "elementIndex"])
data class Bookmark(
    val serviceId: String,
    val postUrl: String,
    val elementIndex: Int,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)