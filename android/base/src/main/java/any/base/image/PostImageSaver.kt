package any.base.image

import android.webkit.MimeTypeMap
import any.base.util.Dirs
import any.base.util.FileUtil
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.datasource.DataSources
import com.facebook.drawee.backends.pipeline.Fresco
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.math.min

object PostImageSaver {
    suspend fun saveToPicturesDir(
        imageFetcher: DownloadedImageFetcher,
        postTitle: String?,
        imageIndex: Int,
        url: String,
    ): Result<String> = withContext(Dispatchers.IO) save@{
        val picturesDir = Dirs.picturesPostImageDownloadDir()
        val dirName = if (postTitle != null) {
            FileUtil.buildValidFatFilename(postTitle)
        } else {
            ""
        }
        val outputDir = File(picturesDir, dirName)
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            return@save Result.failure(IOException("Cannot create the directory: $outputDir"))
        }
        val extension = when (MimeTypeMap.getFileExtensionFromUrl(url)) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg" // Use default .jpg ext
        }
        val filename = "$imageIndex.$extension"
        val outputFile = File(outputDir, filename)

        try {
            saveImageToFile(
                imageFetcher = imageFetcher,
                url = url,
                targetFile = outputFile,
            )
            Result.success("Image saved")
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Cannot save image: ${e.message}"))
        }
    }

    /**
     * Save downloaded or cache image or online image to target file.
     *
     * @return True if saved, false if failed.
     */
    suspend fun saveImageToFile(
        imageFetcher: DownloadedImageFetcher,
        url: String,
        targetFile: File,
    ): Boolean = withContext(Dispatchers.IO) {
        val downloadedFile = imageFetcher.getDownloadedFile(url = url)
        if (downloadedFile != null) {
            downloadedFile.copyTo(target = targetFile, overwrite = true)
            return@withContext true
        }

        val request = ImageRequest.Downloadable(url = url)
        val sources = PostImageSources.all() - PostImageSources.memory()
        val result = ImageLoader.fetchImage(request = request, sources = sources)
            .catch { it.printStackTrace() }
            .firstOrNull() ?: return@withContext false
        when (result) {
            is ImageResult.Bitmap -> {
                throw IllegalStateException("Should not receive a bitmap")
            }
            is ImageResult.File -> {
                val file = result.value
                if (file.absolutePath != targetFile.absolutePath) {
                    file.copyTo(target = targetFile, overwrite = true)
                }
                true
            }
            is ImageResult.InputStream -> {
                result.value.buffered().use {
                    targetFile.outputStream().buffered().use { output ->
                        it.copyTo(output)
                    }
                }
                true
            }
            else -> {
                false
            }
        }
    }

    /**
     * Save (or download) image to target file.
     *
     * @throws Throwable If download has failed
     * @throws IOException If copy has failed
     */
    fun saveImageToFile(
        url: String,
        targetFile: File,
        removeDiskCacheIfCopied: Boolean = false,
    ): Boolean {
        val request = com.facebook.imagepipeline.request.ImageRequest.fromUri(url)!!
        val imageDataSource = Fresco
            .getImagePipeline()
            .fetchEncodedImage(request, null)
        return try {
            // Fetch image synchronously
            val result = DataSources.waitForFinalResult(imageDataSource)
            if (result != null) {
                // Copy fresco buffer to target file
                val copied = copyFrescoBufferToFile(result.get(), targetFile)
                if (copied && removeDiskCacheIfCopied) {
                    // Remove downloaded image from disk cache
                    Fresco.getImagePipeline().evictFromDiskCache(request)
                }
                copied
            } else {
                throw Exception("Cannot fetch image: $url")
            }
        } finally {
            imageDataSource.close()
        }
    }

    private fun copyFrescoBufferToFile(
        buffer: PooledByteBuffer,
        targetFile: File,
    ): Boolean {
        val bufferSize = buffer.size()
        if (bufferSize == 0) {
            return false
        }
        targetFile.outputStream().buffered().use {
            val byteArray = ByteArray(DEFAULT_BUFFER_SIZE)
            var offset = 0
            do {
                val length = min(bufferSize - offset, byteArray.size)
                val readCount = buffer.read(offset, byteArray, 0, length)
                if (readCount > 0) {
                    it.write(byteArray, 0, readCount)
                    offset += readCount
                }
            } while (readCount > 0)
        }
        return true
    }
}