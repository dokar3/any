package any.ui.common

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import any.base.util.applyLightNavBar
import any.base.util.applyLightStatusBar
import any.base.util.clearLightBars
import any.base.util.getActivityOrNull
import any.base.util.setNavigationBarColor
import any.base.util.setStatusBarColor
import any.ui.common.theme.navigationBar
import any.ui.common.theme.statusBar

@Composable
fun TintSystemBars(
    darkMode: Boolean,
    statusBarColor: Color = MaterialTheme.colors.statusBar,
    navigationBarColor: Color = MaterialTheme.colors.navigationBar,
    applyLightStatusBarAutomatically: Boolean = true,
    applyLightNavigationBarAutomatically: Boolean = true,
) {
    val context = LocalContext.current
    LaunchedEffect(
        context,
        darkMode,
        statusBarColor,
        navigationBarColor,
        applyLightStatusBarAutomatically,
        applyLightNavigationBarAutomatically,
    ) {
        val window = context.getActivityOrNull()?.window ?: return@LaunchedEffect
        val statusBarColorInt = if (statusBarColor.alpha == 0f) {
            // Zero alpha colors may get ignored, so set the alpha to 1
            statusBarColor.toArgb() and 0x01ffffff
        } else {
            statusBarColor.toArgb()
        }
        window.setStatusBarColor(statusBarColorInt, animate = false)

        val navigationBarColorInt = if (navigationBarColor.alpha == 0f) {
            // Zero alpha colors may get ignored, so set the alpha to 1
            navigationBarColor.toArgb() and 0x01ffffff
        } else {
            navigationBarColor.toArgb()
        }
        window.setNavigationBarColor(navigationBarColorInt, animate = false)

        val view = window.decorView
        if (darkMode) {
            view.clearLightBars(window)
        } else {
            if (applyLightStatusBarAutomatically) {
                view.applyLightStatusBar(window)
            }
            if (applyLightNavigationBarAutomatically) {
                view.applyLightNavBar(window)
            }
        }
    }
}