package any.base.util

import android.graphics.Color

object ColorUtil {
    fun parseOrDefault(
        hex: String?,
        defaultValue: Int = Color.TRANSPARENT,
    ): Int {
        if (hex == null) {
            return defaultValue
        }
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            defaultValue
        }
    }

    fun parseOrNull(hex: String?): Int? {
        return try {
            Color.parseColor(hex)
        } catch (e: Exception) {
            null
        }
    }
}
