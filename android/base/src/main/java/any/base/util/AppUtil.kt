package any.base.util

import android.content.Context
import android.os.Build

object AppUtil {
    fun getAppVersionName(context: Context): String {
        val packageManager = context.packageManager
        val info = packageManager.getPackageInfo(context.packageName, 0)
        return info.versionName
    }

    fun getAppVersionCode(context: Context): Long {
        val packageManager = context.packageManager
        val info = packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }
    }
}