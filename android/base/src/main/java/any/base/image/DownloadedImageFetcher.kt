package any.base.image

import java.io.File

interface DownloadedImageFetcher {
    fun getDownloadedFile(url: String): File?
}