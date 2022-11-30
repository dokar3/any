package any.base.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.getActivity(): Activity {
    return getActivityOrNull()
        ?: throw IllegalArgumentException("Cannot resolve an activity from $this")
}

fun Context.getActivityOrNull(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) {
            return ctx
        }
        ctx = ctx.baseContext
    }
    return null
}
