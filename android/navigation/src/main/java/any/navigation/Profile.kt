package any.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument

object ProfileArgs : NavArgs {
    override val navArgs = listOf(
        navArgument("serviceId") {
            type = NavType.StringType
            nullable = true
        },
        navArgument("userId") {
            type = NavType.StringType
            nullable = true
        },
        navArgument("userUrl") {
            type = NavType.StringType
            nullable = true
        }
    )
}

val NavBackStackEntry.userUrl: String?
    get() {
        return arguments?.getString("userUrl")
    }

val NavBackStackEntry.userId: String?
    get() {
        return arguments?.getString("userId")
    }

fun Routes.userProfile(serviceId: String, userId: String): String {
    return Routes.Builder(PROFILE)
        .arg("serviceId", serviceId)
        .arg("userId", userId)
        .build()
}

fun Routes.userProfile(userUrl: String): String {
    return Routes.Builder(PROFILE)
        .arg("userUrl", userUrl)
        .build()
}
