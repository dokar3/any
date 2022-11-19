package any.base.util

import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.view.View
import android.view.Window
import android.view.animation.DecelerateInterpolator
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun View.hideBars(window: Window) {
    WindowCompat.getInsetsController(window, this).run {
        systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        hide(WindowInsetsCompat.Type.systemBars())
    }
}

fun View.showBars(window: Window) {
    WindowCompat.getInsetsController(window, this).run {
        show(WindowInsetsCompat.Type.systemBars())
    }
}

fun View.applyLightBars(window: Window) {
    WindowCompat.getInsetsController(window, this).run {
        isAppearanceLightStatusBars = true
        isAppearanceLightNavigationBars = true
    }
}

fun View.applyLightStatusBar(window: Window) {
    WindowCompat.getInsetsController(window, this).run {
        isAppearanceLightStatusBars = true
    }
}

fun View.applyLightNavBar(window: Window) {
    WindowCompat.getInsetsController(window, this).run {
        isAppearanceLightNavigationBars = true
    }
}

fun View.clearLightBars(window: Window) {
    WindowCompat.getInsetsController(window, this).run {
        isAppearanceLightStatusBars = false
        isAppearanceLightNavigationBars = false
    }
}

fun View.clearLightStatusBar(window: Window) {
    WindowCompat.getInsetsController(window, this).run {
        isAppearanceLightStatusBars = false
    }
}

fun View.clearLightNavBar(window: Window) {
    WindowCompat.getInsetsController(window, this).run {
        isAppearanceLightNavigationBars = false
    }
}

fun Context.getStatusBarColor(): Int {
    val activity = activityFromContext(this) ?: return Color.TRANSPARENT
    return activity.window.statusBarColor
}

fun Context.setStatusBarColor(color: Int, animate: Boolean = false) {
    val activity = activityFromContext(this) ?: return
    activity.window.setStatusBarColor(color, animate)
}

fun Window.setStatusBarColor(color: Int, animate: Boolean = false) {
    if (animate) {
        ValueAnimator.ofArgb(statusBarColor, color).apply {
            duration = 355L
            interpolator = DecelerateInterpolator(1.2f)
            addUpdateListener {
                statusBarColor = it.animatedValue as Int
            }
            start()
        }
    } else {
        statusBarColor = color
    }
}

fun Context.getNavigationBarColor(): Int {
    val activity = activityFromContext(this) ?: return Color.TRANSPARENT
    return activity.window.navigationBarColor
}

fun Context.setNavigationBarColor(color: Int, animate: Boolean = false) {
    val activity = activityFromContext(this) ?: return
    activity.window.setNavigationBarColor(color, animate)
}

fun Window.setNavigationBarColor(color: Int, animate: Boolean = false) {
    if (animate) {
        ValueAnimator.ofArgb(navigationBarColor, color).apply {
            duration = 355L
            interpolator = DecelerateInterpolator(1.2f)
            addUpdateListener {
                navigationBarColor = it.animatedValue as Int
            }
            start()
        }
    } else {
        navigationBarColor = color
    }
}

private fun activityFromContext(context: Context): Activity? {
    return when (context) {
        is Activity -> {
            context
        }

        is ContextWrapper -> {
            activityFromContext(context.baseContext)
        }

        else -> {
            null
        }
    }
}