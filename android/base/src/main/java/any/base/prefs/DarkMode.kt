package any.base.prefs

import android.content.Context
import android.content.res.Configuration

enum class DarkMode {
    Yes,
    No,
    System,
}

fun DarkMode.toEnabledState(context: Context): Boolean {
    return when (this) {
        DarkMode.System -> {
            val uiMode = context.resources.configuration.uiMode
            uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }

        DarkMode.Yes -> true
        DarkMode.No -> false
    }
}
