package any.base.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

object ClipboardUtil {

    fun copyText(context: Context, text: String) {
        val clipManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as
                ClipboardManager
        clipManager.setPrimaryClip(ClipData.newPlainText(null, text))
    }
}