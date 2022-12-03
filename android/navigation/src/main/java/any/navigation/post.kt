package any.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import java.net.URLEncoder

object PostArgs : NavArgs {
    override val navArgs = listOf(
        navArgument("postUrl") { type = NavType.StringType },
        navArgument("serviceId") {
            type = NavType.StringType
            nullable = true
        },
        navArgument("initialElementIndex") { type = NavType.IntType },
        navArgument("initialElementScrollOffset") { type = NavType.IntType },
    )
}

val NavBackStackEntry.postUrl: String?
    get() {
        return arguments?.getString("postUrl")
    }

val NavBackStackEntry.serviceId: String?
    get() {
        return arguments?.getString("serviceId")
    }

val NavBackStackEntry.initialElementIndex: Int?
    get() = arguments?.getInt("initialElementIndex", -1)

val NavBackStackEntry.initialElementScrollOffset: Int?
    get() = arguments?.getInt("initialElementScrollOffset", 0)

fun Routes.post(
    url: String,
    serviceId: String?,
    initialElementIndex: Int = -1,
    initialElementScrollOffset: Int = 0,
): String {
    val postUrlEnc = URLEncoder.encode(url, "utf-8")
    return Routes.Builder(POST)
        .arg("postUrl", postUrlEnc)
        .arg("serviceId", serviceId)
        .arg("initialElementIndex", initialElementIndex)
        .arg("initialElementScrollOffset", initialElementScrollOffset)
        .build()
}
