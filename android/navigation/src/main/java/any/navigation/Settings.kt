package any.navigation

import androidx.annotation.StringDef
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.navArgument

@StringDef(
    Routes.Settings.MAIN,
    Routes.Settings.UI,
    Routes.Settings.DEV,
    Routes.Settings.FILES,
    Routes.Settings.SERVICE_MGT,
    Routes.Settings.PRIVACY,
    Routes.Settings.ABOUT,
    Routes.Settings.LIBS,
)
annotation class SettingsRoute

object SettingsArgs : NavArgs {
    override val navArgs = listOf(
        navArgument("subSettings") {
            type = NavType.StringType
            nullable = true
        }
    )
}

val NavBackStackEntry.subSettings: String?
    get() {
        return arguments?.getString("subSettings")
    }

fun Routes.settings(
    @SettingsRoute subSettings: String? = null,
): String {
    return Routes.Builder(SETTINGS)
        .arg("subSettings", subSettings)
        .build()
}

