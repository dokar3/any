package any.base.image

import com.facebook.imagepipeline.request.ImageRequest as FrescoRequest
import android.graphics.Bitmap
import android.util.Size
import com.facebook.imagepipeline.common.ResizeOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class ImageFetcher(
    private val downloadedFetcher: DownloadedImageFetcher,
    private val subsamplingImageCache: SubsamplingImageCache?,
) {
    fun fetchBitmap(
        request: ImageRequest,
        size: Size?,
        finalResultOnly: Boolean = false,
    ): Flow<Bitmap> {
        val frescoRequest = request.toFrescoRequest(size)

        val cached = FrescoUtil.fetchFromBitmapCache(frescoRequest)
        if (cached != null) {
            return flowOf(cached)
        }

        return FrescoUtil.fetchBitmaps(frescoRequest)
            .filter { (_, isFinialResult) -> !finalResultOnly || isFinialResult }
            .map { it.first }
    }

    fun fetchBitmapFromCache(request: ImageRequest, size: Size?): Bitmap? {
        return FrescoUtil.fetchFromBitmapCache(request.toFrescoRequest(size))
    }

    private fun ImageRequest.toFrescoRequest(size: Size?): FrescoRequest {
        return if (size != null) {
            val resizeOptions = ResizeOptions(size.width, size.height)
            toFrescoRequestBuilder()
                .setResizeOptions(resizeOptions)
                .build()
        } else {
            toFrescoRequestBuilder().build()
        }
    }

    /**
     * Load image from specific sources.
     *
     * [ImageResult.Bitmap] will be emitted if there is a bitmap in the memory cache or progressive
     * rendering bitmaps are available.
     *
     * [ImageResult.File] will be emitted if there is an image in the disk cache,
     *
     * [ImageResult.File] or [ImageResult.InputStream] will be emitted if image was fetched from
     * network.
     *
     * [ImageResult.Failure] will be emitted if nothing is loaded.
     *
     * @param request the image request.
     * @param sources the image sources.
     */
    fun fetchImage(
        request: ImageRequest.Downloadable,
        size: Size?,
        sources: PostImageSources,
    ): Flow<ImageResult> = channelFlow {
        require(sources != PostImageSources.none()) { "None of image sources are configured" }

        val frescoRequest = request.toFrescoRequestBuilder().build()

        var haveSentBitmap = false

        suspend fun trySendCacheFile(): Boolean {
            val cachedFile = FrescoUtil.fetchCachedFile(frescoRequest)
            return if (cachedFile != null) {
                send(ImageResult.File(cachedFile))
                true
            } else {
                false
            }
        }

        if (sources.contains(PostImageSources.memory())) {
            // Check in-memory bitmap first
            val bitmap = fetchBitmapFromCache(request, size)
            if (bitmap != null) {
                haveSentBitmap = true
                send(ImageResult.Bitmap(bitmap))
            }
        }

        if (sources.contains(PostImageSources.downloadDir())) {
            // Check if image is already downloaded
            val downloadFile = downloadedFetcher.getDownloadedFile(
                url = request.url
            )
            if (downloadFile != null) {
                send(ImageResult.File(downloadFile))
                channel.close()
                return@channelFlow
            }
        }

        if (sources.contains(PostImageSources.diskCache())) {
            // Check fresco disk cache
            if (trySendCacheFile()) {
                channel.close()
                return@channelFlow
            }
        }

        if (sources.contains(PostImageSources.subsamplingCacheDir())) {
            // Check subsampling cache
            val subsamplingCacheFile = subsamplingImageCache?.get(request.url)
            if (subsamplingCacheFile != null) {
                send(ImageResult.File(subsamplingCacheFile))
                channel.close()
                return@channelFlow
            }
        }

        if (sources.contains(PostImageSources.network())) {
            // Load the image from network or cache
            FrescoUtil.fetchBitmaps(frescoRequest).collect { (bitmap, isFinalResult) ->
                if (!haveSentBitmap && !isFinalResult) {
                    // Send progressive rendering bitmaps
                    trySend(ImageResult.Bitmap(bitmap))
                }
            }

            delay(20)

            if (trySendCacheFile()) {
                channel.close()
                return@channelFlow
            }

            // Fetch the input stream if disk cache is not available yet
            val inputStream = FrescoUtil.fetchInputStream(frescoRequest)
            if (inputStream != null) {
                delay(20)

                if (trySendCacheFile()) {
                    // Close input stream since we are not using it
                    inputStream.close()
                    channel.close()
                    return@channelFlow
                }

                send(ImageResult.InputStream(inputStream))
                channel.close()
            } else {
                send(ImageResult.Failure(Exception("Cannot fetch this image")))
                channel.close()
            }
        }
    }.flowOn(Dispatchers.IO)
}