package any.base.image

import android.net.Uri
import com.facebook.imagepipeline.request.ImageRequestBuilder
import java.io.File

fun ImageRequest.toFrescoRequestBuilder(): ImageRequestBuilder {
    return when (this) {
        is ImageRequest.Url -> {
            val url = this.url
            val file = File(url)
            if (!file.exists() || !file.isFile) {
                val uri = if (url.startsWith("file:///android_asset")) {
                    val assetUrl = "asset://${url.removePrefix("file:///android_asset")}"
                    Uri.parse(assetUrl)
                } else {
                    Uri.parse(url)
                }
                ImageRequestBuilder.newBuilderWithSource(uri)
                    .applyCommonFromRequest(this)
                    .setProgressiveRenderingEnabled(true)
            } else {
                ImageRequest.File(file).toFrescoRequestBuilder()
            }
        }

        is ImageRequest.Uri -> {
            ImageRequestBuilder.newBuilderWithSource(uri)
                .applyCommonFromRequest(this)
                .setProgressiveRenderingEnabled(true)
        }

        is ImageRequest.Res -> {
            ImageRequestBuilder.newBuilderWithResourceId(resId)
                .applyCommonFromRequest(this)
        }

        is ImageRequest.File -> {
            ImageRequestBuilder.newBuilderWithSource(Uri.fromFile(file))
                .applyCommonFromRequest(this)
        }

        is ImageRequest.Downloadable -> {
            ImageRequestBuilder.newBuilderWithSource(Uri.parse(url))
                .applyCommonFromRequest(this)
                .setProgressiveRenderingEnabled(true)
        }
    }
}

fun ImageRequest.Downloadable.frescoRequestBuilders(
    imageFetcher: DownloadedImageFetcher,
): Array<ImageRequestBuilder> {
    val downloadedFile = imageFetcher.getDownloadedFile(url = url)
    val urlRequest = toFrescoRequestBuilder()
    return if (downloadedFile != null) {
        val fileRequest = ImageRequestBuilder
            .newBuilderWithSource(Uri.fromFile(downloadedFile))
        arrayOf(fileRequest, urlRequest)
    } else {
        arrayOf(urlRequest)
    }
}

private fun ImageRequestBuilder.applyCommonFromRequest(
    request: ImageRequest
): ImageRequestBuilder {
    return runIf(!request.memoryCacheEnabled) { disableMemoryCache() }
        .runIf(!request.diskCacheEnabled) { disableDiskCache() }
}

private inline fun <T : Any> T.runIf(value: Boolean, block: T.() -> T): T {
    return if (value) block(this) else this
}
