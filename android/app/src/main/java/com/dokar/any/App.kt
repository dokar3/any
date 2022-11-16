package com.dokar.any

import android.app.Application
import android.os.Build
import android.os.StrictMode
import any.base.image.ImageFetcher
import any.base.image.ImageLoader
import any.base.log.Logger
import any.base.log.NoopLogger
import any.base.util.CrashHandler
import any.data.cache.SubsamplingImageCache
import any.download.PostImageDownloader

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.init(this)
        setupLogger()
        setupStrictMode()
        setupImageLoader()
    }

    private fun setupLogger() {
        if (!BuildConfig.DEBUG) {
            Logger.logger = NoopLogger
        }
    }

    private fun setupStrictMode() {
        if (!BuildConfig.DEBUG) {
            return
        }
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectActivityLeaks()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .detectLeakedSqlLiteObjects()
                .let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        it.detectIncorrectContextUse()
                    } else {
                        it
                    }
                }
                .build()
        )
    }

    private fun setupImageLoader() {
        val imageFetcher = ImageFetcher(
            downloadedFetcher =  PostImageDownloader.get(this),
            subsamplingImageCache = SubsamplingImageCache.get(this),
        )
        ImageLoader.setup(this, imageFetcher)
    }
}