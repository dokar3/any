package any.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController

fun NavController.navigateAndReplace(route: String) {
    navigate(route) {
        popUpTo(currentDestination!!.id) {
            inclusive = true
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

fun NavController.popBackStackUtil(predicate: (NavBackStackEntry)-> Boolean) {
    var curr = currentBackStackEntry
    while (curr != null && !predicate(curr)) {
        popBackStack()
        curr = currentBackStackEntry
    }
}
