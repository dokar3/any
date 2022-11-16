package any.base

import android.content.Context
import any.base.util.AppUtil

interface AppVersionProvider {
    val versionName: String

    val versionCode: Long
}

class DefaultAppVersionProvider(context: Context) : AppVersionProvider {
    private val appContext = context.applicationContext

    override val versionName: String
        get() = AppUtil.getAppVersionName(appContext)

    override val versionCode: Long
        get() = AppUtil.getAppVersionCode(appContext)
}