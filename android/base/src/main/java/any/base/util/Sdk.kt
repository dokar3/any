package any.base.util

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

object Sdk {
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.M)
    fun hasAndroidM() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O_MR1)
    fun hasAndroidO() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
    fun hasAndroidQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
    fun hasAndroidT() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}