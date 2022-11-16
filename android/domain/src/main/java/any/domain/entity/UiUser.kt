package any.domain.entity

import androidx.compose.runtime.Immutable
import any.data.entity.User
import any.richtext.RichContent

@Immutable
data class UiUser(
    val raw: User,
    val serviceName: String?,
    val serviceIcon: String?,
    val serviceThemeColor: Int,
    val serviceDarkThemeColor: Int,
    val description: RichContent?,
) {
    val serviceId: String get() = raw.serviceId
    val id: String get() = raw.id
    val name: String get() = raw.name
    val alternativeName: String? get() = raw.alternativeName
    val url: String? get() = raw.url
    val avatar: String? get() = raw.avatar
    val banner: String? get() = raw.banner
    val postCount: Int? get() = raw.postCount
    val followerCount: Int? get() = raw.followerCount
    val followingCount: Int? get() = raw.followingCount
    val followedAt: Long get() = raw.followedAt
    val group: String? get() = raw.group

    fun isFollowed() = raw.isFollowed()

    companion object
}
