package any.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import java.net.URLDecoder
import java.net.URLEncoder

object SearchArgs : NavArgs {
    override val navArgs = listOf(
        navArgument("serviceId") {
            type = NavType.StringType
            nullable = true
        },
        navArgument("query") {
            type = NavType.StringType
            nullable = true
        }
    )
}

val NavBackStackEntry.query: String?
    get() {
        return arguments
            ?.getString("query")
            ?.let { URLDecoder.decode(it, "utf-8") }
    }

fun Routes.search(serviceId: String?, query: String? = null): String {
    return Routes.Builder(SEARCH)
        .arg("serviceId", serviceId)
        .arg("query", URLEncoder.encode(query ?: "", "utf-8"))
        .build()
}
