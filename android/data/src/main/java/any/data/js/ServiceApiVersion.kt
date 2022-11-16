package any.data.js

import any.data.BuildConfig

object ServiceApiVersion {
    /**
     * Get the version of 'any-api' module
     */
    fun get(): String = BuildConfig.JS_SERVICE_API_VERSION
}