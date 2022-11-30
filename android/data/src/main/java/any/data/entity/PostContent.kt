package any.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity
class PostContent(
    @PrimaryKey
    val url: String,
    val elements: List<ContentElement>,
)