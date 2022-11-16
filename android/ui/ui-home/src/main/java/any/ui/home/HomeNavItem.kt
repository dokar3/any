package any.ui.home

import any.navigation.Routes
import any.ui.common.R

sealed class HomeNavItem(
    val name: String,
    val icon: Int,
    val route: String
) {
    class Fresh(
        name: String,
    ) : HomeNavItem(
        name = name,
        icon = R.drawable.ic_fresh,
        route = Routes.Home.FRESH,
    )

    class Following(
        name: String,
    ) : HomeNavItem(
        name = name,
        icon = R.drawable.ic_following,
        route = Routes.Home.FOLLOWING,
    )

    class Collections(
        name: String,
    ) : HomeNavItem(
        name = name,
        icon = R.drawable.ic_collection,
        route = Routes.Home.COLLECTIONS,
    )

    class Downloads(
        name: String,
    ) : HomeNavItem(
        name = name,
        icon = R.drawable.ic_download,
        route = Routes.Home.DOWNLOADS
    )
}