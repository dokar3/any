package any.base.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object AppUtil {
    @Suppress("deprecation")
    fun getAppVersionName(context: Context): String {
        val packageManager = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            packageManager.getPackageInfo(context.packageName, 0)
        }
        return info.versionName!!
    }

    @Suppress("deprecation")
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