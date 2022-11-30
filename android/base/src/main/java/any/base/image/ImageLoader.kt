package any.base.image

import android.app.Application
import android.content.ComponentCallbacks2
import android.graphics.Bitmap
import android.util.Size
import any.base.AutoCleaner
import any.base.util.Http
import com.facebook.cache.disk.DiskCacheConfig
import com.facebook.common.memory.MemoryTrimType
import com.facebook.common.memory.MemoryTrimmable
import com.facebook.common.memory.MemoryTrimmableRegistry
import com.facebook.common.util.ByteConstants
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory
import kotlinx.coroutines.flow.Flow

object ImageLoader {
    private const val MAX_DISK_CACHE_MB = 1024

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

    fun setup(app: Application, imageFetcher: ImageFetcher) {
        if (Fresco.hasBeenInitialized()) {
            throw IllegalStateException("ImageLoader is already set up")
        }
        val diskCacheConfig = DiskCacheConfig.newBuilder(app)
            .setMaxCacheSize(MAX_DISK_CACHE_MB.toLong() * ByteConstants.MB)
            .build()
        val pipelineConfig = OkHttpImagePipelineConfigFactory
            .newBuilder(app, Http.DEFAULT_CLIENT)
            .setDiskCacheEnabled(true)
            .setDownsampleEnabled(true)
            .setResizeAndRotateEnabledForNetwork(true)
            .setMainDiskCacheConfig(diskCacheConfig)
            .setMemoryTrimmableRegistry(memoryTrimmableRegistry)
            .build()
        Fresco.initialize(app, pipelineConfig)
        this.imageFetcher = imageFetcher
    }

    fun findRequestSize(request: ImageRequest): Size? {
        return requestSizes[request]
    }

    fun attachRequest(request: ImageRequest, size: Size? = null) {
        if (size != null && size.width > 0 && size.height > 0) {
            requestSizes[request] = size
        }
        autoCleaner.enqueue(request)
    }

    fun detachRequest(request: ImageRequest) {
        autoCleaner.remove(request)
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
