package any.ui.home.following

import androidx.compose.runtime.Immutable

@Immutable
data class ServiceOfFollowingUsers(
    val id: String = "",
    val name: String = "",
    val icon: String? = null,
    val userCount: Int = 0,
    val themeColor: Int = 0,
    val darkThemeColor: Int = 0,
    val isSelected: Boolean = false,
    val isAll: Boolean = false,
)
