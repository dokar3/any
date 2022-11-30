package any.base

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Stable

@Stable
class AndroidStrings(context: Context) : Strings {
    private val resources = context.applicationContext.resources

    override fun invoke(@StringRes id: Int): String {
        return resources.getString(id)
    }

    override fun invoke(@StringRes id: Int, vararg formatArgs: Any): String {
        return resources.getString(id, *formatArgs)
    }
}