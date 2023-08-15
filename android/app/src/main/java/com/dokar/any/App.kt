package com.dokar.any

import android.app.Application
import android.os.Build
import android.os.StrictMode
import any.base.image.ImageFetcher
import any.base.image.ImageLoader
import any.base.image.SubsamplingImageCache
import any.base.log.Logger
import any.base.log.NoOpLogger
import any.base.prefs.maxImageCacheSize
import any.base.prefs.preferencesStore
import any.base.util.CrashHandler
import any.download.PostImageDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class App : Application() {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        CrashHandler.init(this)
        setupLogger()
        setupStrictMode()
        setupImageLoader()
    }

    private fun setupLogger() {
        if (!BuildConfig.DEBUG && !BuildConfig.BENCHMARK) {
            Logger.logger = NoOpLogger
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
            downloadedFetcher = PostImageDownloader.get(this),
            subsamplingImageCache = SubsamplingImageCache.get(this),
        )
        val preferencesStore = this.preferencesStore()
        ImageLoader.setup(
            app = this,
            imageFetcher = imageFetcher,
            maxDiskCacheSize = preferencesStore.maxImageCacheSize.value,
        )
        coroutineScope.launch {
            // Listen max image cache size
            preferencesStore.maxImageCacheSize.asFlow()
                .drop(1) // Prevent duplicate initializations on start up
                .filter { it > 0 }
                .collect {
                    // Re-initialize image loader
                    ImageLoader.setup(
                        app = this@App,
                        imageFetcher = imageFetcher,
                        maxDiskCacheSize = it,
                    )
                }
        }
    }
}