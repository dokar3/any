package any.base.image

import android.graphics.Bitmap
import any.base.log.Logger
import com.facebook.binaryresource.FileBinaryResource
import com.facebook.common.executors.CallerThreadExecutor
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.memory.PooledByteBufferInputStream
import com.facebook.common.references.CloseableReference
import com.facebook.datasource.DataSource
import com.facebook.datasource.DataSources
import com.facebook.datasource.DataSubscriber
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.cache.DefaultCacheKeyFactory
import com.facebook.imagepipeline.core.ImagePipelineFactory
import com.facebook.imagepipeline.image.CloseableAnimatedImage
import com.facebook.imagepipeline.image.CloseableBitmap
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.imagepipeline.request.ImageRequest
import java.io.File
import java.io.InputStream
import java.util.concurrent.CountDownLatch

object FrescoUtil {
    fun fetchCachedFile(request: ImageRequest): File? {
        val cacheKey = DefaultCacheKeyFactory.getInstance()
            .getEncodedCacheKey(request, null)
        val mainCache = ImagePipelineFactory.getInstance().mainFileCache
        val smallCache = ImagePipelineFactory.getInstance().smallImageFileCache
        return if (mainCache.hasKey(cacheKey)) {
            val resource = mainCache.getResource(cacheKey)
            (resource as? FileBinaryResource)?.file
        } else if (smallCache.hasKey(cacheKey)) {
            val resource = smallCache.getResource(cacheKey)
            (resource as? FileBinaryResource)?.file
        } else {
            null
        }
    }

    fun fetchInputStream(request: ImageRequest): InputStream? {
        val dataSource = Fresco.getImagePipeline().fetchEncodedImage(request, null)
        try {
            val ref = DataSources.waitForFinalResult(dataSource)
            if (ref != null) {
                return ReferencedPooledByteBufferInputStream(ref)
            }
        } finally {
            dataSource.close()
        }
        return null
    }

    fun fetchFromBitmapCache(request: ImageRequest): Bitmap? {
        val dataSource = Fresco.getImagePipeline().fetchImageFromBitmapCache(request, null)
        var bitmap: Bitmap? = null
        try {
            dataSource.result?.use { ref ->
                val result = ref.get()
                if (result is CloseableBitmap) {
                    val bmp = result.underlyingBitmap
                    bitmap = bmp.copy(bmp.config, false)
                }
            }
        } finally {
            dataSource.close()
        }
        return bitmap
    }

    /**
     * Fetch bitmap(s), this method is synchronous.
     */
    fun fetchBitmaps(
        request: ImageRequest,
        onBitmap: (bitmap: Bitmap, isFinalResult: Boolean) -> Unit,
    ) {
        if (request.sourceUri.toString().isEmpty()) {
            return
        }
        val latch = CountDownLatch(1)
        val source = Fresco.getImagePipeline().fetchDecodedImage(request, null)
        try {
            val subscriber = object : DataSubscriber<CloseableReference<CloseableImage>> {
                override fun onNewResult(
                    dataSource: DataSource<CloseableReference<CloseableImage>>
                ) {
                    dataSource.result?.use { ref ->
                        val image = ref.get()
                        if (image is CloseableBitmap) {
                            val bitmap = image.underlyingBitmap
                            onBitmap(bitmap.copy(bitmap.config, false), dataSource.isFinished)
                        } else if (image is CloseableAnimatedImage) {
                            val firstFrame = image.image?.getFrame(0)
                            if (firstFrame != null) {
                                val width = firstFrame.width
                                val height = firstFrame.height
                                val bitmap = Bitmap.createBitmap(
                                    width,
                                    height,
                                    image.image!!.animatedBitmapConfig ?: Bitmap.Config.ARGB_8888,
                                )
                                firstFrame.renderFrame(width, height, bitmap)
                                onBitmap(bitmap, dataSource.isFinished)
                            }
                        } else {
                            Logger.w("FrescoUtil", "fetchBitmaps(): Unsupported result: $image")
                        }
                    }
                    if (dataSource.isFinished) {
                        latch.countDown()
                    }
                }

                override fun onFailure(
                    dataSource: DataSource<CloseableReference<CloseableImage>>
                ) {
                    latch.countDown()
                    dataSource.failureCause?.let { error ->
                        throw error
                    }
                }

                override fun onCancellation(
                    dataSource: DataSource<CloseableReference<CloseableImage>>
                ) {
                    latch.countDown()
                }

                override fun onProgressUpdate(
                    dataSource: DataSource<CloseableReference<CloseableImage>>
                ) {
                }
            }

            source.subscribe(subscriber, CallerThreadExecutor.getInstance())

            latch.await()
        } finally {
            source.close()
        }
    }
}

private class ReferencedPooledByteBufferInputStream(
    private val ref: CloseableReference<PooledByteBuffer>,
) : PooledByteBufferInputStream(ref.get()) {
    override fun close() {
        super.close()
        ref.close()
    }
}
