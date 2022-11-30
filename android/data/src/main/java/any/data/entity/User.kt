package any.data.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import com.squareup.moshi.JsonClass

@Immutable
@Entity(primaryKeys = ["serviceId", "id"])
@JsonClass(generateAdapter = true)
data class User(
    val serviceId: String,
    val id: String,
    val name: String,
    val alternativeName: String?,
    val url: String?,
    val avatar: String?,
    val banner: String?,
    val description: String?,
    val postCount: Int?,
    val followerCount: Int?,
    val followingCount: Int?,
    val pageKeyOfPage2: JsPageKey? = null,
    val followedAt: Long = -1L,
    val group: String? = null,
) {
    fun markFollowed(at: Long = System.currentTimeMillis()): User {
        return copy(followedAt = at)
    }

    fun markUnfollowed(): User {
        return copy(followedAt = -1)
    }

    fun isFollowed(): Boolean = followedAt >= 0

    companion object {
        internal fun fromJsUser(
            serviceId: String,
            jsUser: JsUser,
            pageKeyOfPage2: JsPageKey? = null,
            followedAt: Long = -1L,
            group: String? = null,
        ): User {
            return User(
                serviceId = serviceId,
                id = jsUser.id,
                name = jsUser.name,
                alternativeName = jsUser.alternativeName,
                url = jsUser.url,
                avatar = jsUser.avatar,
                banner = jsUser.banner,
                description = jsUser.description,
                postCount = jsUser.postCount,
                followerCount = jsUser.followerCount,
                followingCount = jsUser.followingCount,
                pageKeyOfPage2 = pageKeyOfPage2,
                followedAt = followedAt,
                group = group,
            )
        }
    }
}
