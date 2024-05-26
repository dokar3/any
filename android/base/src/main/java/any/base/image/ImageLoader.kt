package any.base.image

import android.app.Application
import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.util.Size
import any.base.AutoCleaner
import any.base.util.Http
import any.base.util.MB
import com.facebook.cache.disk.DiskCacheConfig
import com.facebook.common.memory.MemoryTrimType
import com.facebook.common.memory.MemoryTrimmable
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory
import kotlinx.coroutines.flow.Flow

object ImageLoader {
    private const val HTTP_REQUEST_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private val MAX_DISK_CACHE_SIZE = 1024.MB

    private val trimmables = mutableListOf<MemoryTrimmable>()

    private val requestSizes = mutableMapOf<ImageRequest, Size>()

    private val autoCleaner = AutoCleaner<ImageRequest> {
        val uri = it.toFrescoRequestBuilder().build().sourceUri
        Fresco.getImagePipeline().evictFromMemoryCache(uri)
        requestSizes.remove(it)
    }

    private val memoryTrimmableRegistry = object : MemoryTrimmableRegistry {
        override fun registerMemoryTrimmable(trimmable: MemoryTrimmable?) {
            if (trimmable != null) {
                trimmables.add(trimmable)
            }
        }

        override fun unregisterMemoryTrimmable(trimmable: MemoryTrimmable?) {
            if (trimmable != null) {
                trimmables.remove(trimmable)
            }
        }
    }

    private var imageFetcher: ImageFetcher? = null

    /**
     * Trim memory
     *
     * @see [ComponentCallbacks2.onTrimMemory]
     */
    fun trimMemory(level: Int) {
        val list = trimmables.toList()
        val trimType = when (level) {
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                MemoryTrimType.OnSystemMemoryCriticallyLowWhileAppInForeground
            }

            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
            ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                MemoryTrimType.OnAppBackgrounded
            }

            else -> {
                MemoryTrimType.OnSystemLowMemoryWhileAppInForeground
            }
        }
        for (trimmable in list) {
            trimmable.trim(trimType)
        }
    }

    fun setup(
        app: Application,
        imageFetcher: ImageFetcher,
        maxDiskCacheSize: Long = MAX_DISK_CACHE_SIZE,
        force: Boolean = false,
    ) {
        if (isSetup() && !force) return
        this.imageFetcher = imageFetcher
        val diskCacheConfig = DiskCacheConfig.newBuilder(app)
            .setMaxCacheSize(
                if (maxDiskCacheSize > 0L) {
                    maxDiskCacheSize
                } else {
                    // Fallback or default
                    MAX_DISK_CACHE_SIZE
                }
            )
            .build()
        val httpClient = Http.DEFAULT_CLIENT_BUILDER
            .addInterceptor { chain ->
                val request = chain.request()
                    .newBuilder()
                    .header("User-Agent", HTTP_REQUEST_UA)
                    .build()
                chain.proceed(request)
            }
            .build()
        val pipelineConfig = OkHttpImagePipelineConfigFactory
            .newBuilder(app, httpClient)
            .setDiskCacheEnabled(true)
            .setDownsampleEnabled(true)
            .setResizeAndRotateEnabledForNetwork(true)
            .setMainDiskCacheConfig(diskCacheConfig)
            .setMemoryTrimmableRegistry(memoryTrimmableRegistry)
            .build()
        if (Fresco.hasBeenInitialized()) {
            Fresco.shutDown()
        }
        Fresco.initialize(app, pipelineConfig)
    }

    fun isSetup(): Boolean {
        return imageFetcher != null
    }

    fun findRequestSize(request: ImageRequest): Size? {
        return requestSizes[request]
    }

    fun attachRequest(request: ImageRequest, size: Size? = null) {
        if (size != null && size.width > 0 && size.height > 0) {
            requestSizes[request] = size
        }
        autoCleaner.remove(request)
    }

    fun detachRequest(request: ImageRequest) {
        autoCleaner.enqueue(request)
    }

    fun evictFromCache(request: ImageRequest) {
        val frescoReq = request.toFrescoRequestBuilder().build()
        val uri = frescoReq.sourceUri
        Fresco.getImagePipeline().evictFromCache(uri)
    }

    fun evictFromMemoryCache(request: ImageRequest) {
        val frescoRequest = request.toFrescoRequestBuilder().build()
        val uri = frescoRequest.sourceUri
        Fresco.getImagePipeline().evictFromMemoryCache(uri)
    }

    fun evictFromDiskCache(request: ImageRequest) {
        val frescoRequest = request.toFrescoRequestBuilder().build()
        Fresco.getImagePipeline().evictFromDiskCache(frescoRequest)
    }

    fun fetchBitmap(request: ImageRequest, finalResultOnly: Boolean = false): Flow<Bitmap> {
        return requireNotNull(imageFetcher) { "ImageLoader is not set up yet" }
            .fetchBitmap(request, findRequestSize(request), finalResultOnly)
    }

    fun fetchBitmapFromCache(request: ImageRequest): Bitmap? {
        return requireNotNull(imageFetcher) { "ImageLoader is not set up yet" }
            .fetchBitmapFromCache(request, findRequestSize(request))
    }

    /**
     * Load image from specific sources.
     *
     * @param request the image request.
     * @param sources the image sources.
     *
     * @see [ImageFetcher.fetchImage]
     */
    fun fetchImage(
        request: ImageRequest.Downloadable,
        sources: PostImageSources = PostImageSources.all(),
    ): Flow<ImageResult> {
        attachRequest(request)
        return requireNotNull(imageFetcher) { "ImageLoader is not set up yet" }
            .fetchImage(
                request = request,
                size = findRequestSize(request),
                sources = sources,
            )
    }
}
