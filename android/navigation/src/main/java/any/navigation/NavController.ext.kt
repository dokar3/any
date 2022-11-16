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
    val backQueue = backQueue.reversed()
    for (entry in backQueue) {
        if (!predicate(entry)) {
            popBackStack()
        } else {
            break
        }
    }
}
