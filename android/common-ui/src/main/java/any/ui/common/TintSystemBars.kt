package any.ui.common

import android.app.Activity
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import any.base.util.applyLightNavBar
import any.base.util.applyLightStatusBar
import any.base.util.clearLightBars
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
    val window = (context as Activity).window
    val view = LocalView.current.rootView
    LaunchedEffect(view, darkMode) {
        context.setStatusBarColor(statusBarColor.toArgb(), animate = false)
        context.setNavigationBarColor(navigationBarColor.toArgb(), animate = false)
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