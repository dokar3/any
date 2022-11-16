package any.base.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

fun Context.asActivity(): Activity {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.asActivity()
        else -> throw IllegalArgumentException("Cannot cast to Activity: $this")
    }
}