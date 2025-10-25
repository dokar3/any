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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.io.File
import java.io.InputStream

object FrescoUtil {
    private const val TAG = "FrescoUtil"

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
                    bitmap = bmp.copy(bmp.config!!, false)
                }
            }
        } finally {
            dataSource.close()
        }
        return bitmap
    }

    fun fetchBitmaps(request: ImageRequest): Flow<Pair<Bitmap, Boolean>> = callbackFlow {
        if (request.sourceUri.toString().isEmpty()) {
            channel.close()
            return@callbackFlow
        }

        val source = Fresco.getImagePipeline().fetchDecodedImage(request, null)

        val subscriber = object : DataSubscriber<CloseableReference<CloseableImage>> {
            override fun onNewResult(
                dataSource: DataSource<CloseableReference<CloseableImage>>
            ) {
                dataSource.result?.use { ref ->
                    when (val image = ref.get()) {
                        is CloseableBitmap -> {
                            val bitmap = image.underlyingBitmap.let {
                                it.copy(it.config!!, false)
                            }
                            trySend(bitmap to dataSource.isFinished)
                        }

                        is CloseableAnimatedImage -> {
                            val firstFrame = image.image?.getFrame(0) ?: return@use
                            val width = firstFrame.width
                            val height = firstFrame.height
                            val bitmap = Bitmap.createBitmap(
                                width,
                                height,
                                image.image!!.animatedBitmapConfig ?: Bitmap.Config.ARGB_8888,
                            )
                            firstFrame.renderFrame(width, height, bitmap)
                            trySend(bitmap to dataSource.isFinished)
                        }

                        else -> {
                            Logger.w(TAG, "fetchBitmaps(): Unsupported result: $image")
                        }
                    }
                }
                if (dataSource.isFinished) {
                    channel.close()
                    dataSource.close()
                }
            }

            override fun onFailure(
                dataSource: DataSource<CloseableReference<CloseableImage>>
            ) {
                channel.close()
                dataSource.close()
                dataSource.failureCause?.let { error ->
                    throw error
                }
            }

            override fun onCancellation(
                dataSource: DataSource<CloseableReference<CloseableImage>>
            ) {
                channel.close()
                dataSource.close()
            }

            override fun onProgressUpdate(
                dataSource: DataSource<CloseableReference<CloseableImage>>
            ) {
            }
        }
        source.subscribe(subscriber, CallerThreadExecutor.getInstance())

        awaitClose { source.close() }
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
