package any.base.util

import android.content.Context
import android.content.pm.PackageManager

object PackageUtil {
    const val PKG_GOOGLE_LENS = "com.google.ar.lens"

    fun isPackageInstalled(packageName: String, context: Context): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}