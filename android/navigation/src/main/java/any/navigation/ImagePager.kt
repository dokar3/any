package any.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument
import java.net.URLDecoder
import java.net.URLEncoder

object ImagePagerArgs : NavArgs {
    override val navArgs = listOf(
        navArgument("title") {
            type = NavType.StringType
            defaultValue = ""
        },
        navArgument("currPage") {
            type = NavType.IntType
            defaultValue = 0
        }
    )
}

val NavBackStackEntry.imagePagerTitle: String?
    get() {
        return URLDecoder.decode(arguments?.getString("title") ?: "", "utf-8")
    }

val NavBackStackEntry.imagePagerPage: Int
    get() {
        return arguments?.getInt("currPage", 0) ?: 0
    }

fun Routes.imagePager(title: String?, currPage: Int): String {
    val t = (title ?: "")
        .replace("%", "%25")
        .replace("+", "%2b")
    val encodedTitle = URLEncoder.encode(t, "utf-8")
    return Routes.Builder(IMAGE_PAGER)
        .arg("title", encodedTitle)
        .arg("currPage", currPage.toString())
        .build()
}
