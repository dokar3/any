package any.base.image

import androidx.compose.runtime.Immutable

@Immutable
sealed class ImageRequest(
    val memoryCacheEnabled: Boolean = true,
    val diskCacheEnabled: Boolean = true,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageRequest

        if (memoryCacheEnabled != other.memoryCacheEnabled) return false
        if (diskCacheEnabled != other.diskCacheEnabled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = memoryCacheEnabled.hashCode()
        result = 31 * result + diskCacheEnabled.hashCode()
        return result
    }

    abstract class RequestBuilder<T> {
        protected var memoryCacheEnabled: Boolean = true
            private set
        protected var diskCacheEnabled: Boolean = true
            private set

        fun memoryCacheEnabled(enabled: Boolean): RequestBuilder<T> {
            this.memoryCacheEnabled = enabled
            return this
        }

        fun diskCacheEnabled(enabled: Boolean): RequestBuilder<T> {
            this.diskCacheEnabled = enabled
            return this
        }

        abstract fun build(): T
    }

    /**
     * The universal image request that supports http url, file path and uri string.
     */
    @Immutable
    class Url(
        val url: String,
        memoryCacheEnabled: Boolean = true,
        diskCacheEnabled: Boolean = true,
    ) : ImageRequest(memoryCacheEnabled, diskCacheEnabled) {
        override fun toString(): String {
            return "Url($url)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            other as Url

            if (url != other.url) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + url.hashCode()
            return result
        }

        class Builder(private val url: String) : RequestBuilder<Url>() {
            override fun build(): Url {
                return Url(
                    url = url,
                    memoryCacheEnabled = memoryCacheEnabled,
                    diskCacheEnabled = diskCacheEnabled
                )
            }
        }
    }

    /**
     * Image request to load an image [android.net.Uri].
     */
    @Immutable
    class Uri(
        val uri: android.net.Uri,
        memoryCacheEnabled: Boolean = true,
        diskCacheEnabled: Boolean = true,
    ) : ImageRequest(memoryCacheEnabled, diskCacheEnabled) {
        override fun toString(): String {
            return "Uri($uri)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            other as Uri

            if (uri != other.uri) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + uri.hashCode()
            return result
        }

        class Builder(private val uri: android.net.Uri) : RequestBuilder<Uri>() {
            override fun build(): Uri {
                return Uri(
                    uri = uri,
                    memoryCacheEnabled = memoryCacheEnabled,
                    diskCacheEnabled = diskCacheEnabled
                )
            }
        }
    }

    /**
     * Image request to load an image resource.
     */
    @Immutable
    class Res(
        val resId: Int,
        memoryCacheEnabled: Boolean = true,
        diskCacheEnabled: Boolean = true,
    ) : ImageRequest(memoryCacheEnabled, diskCacheEnabled) {
        override fun toString(): String {
            return "Res($resId)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            other as Res

            if (resId != other.resId) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + resId
            return result
        }

        class Builder(private val resId: Int) : RequestBuilder<Res>() {
            override fun build(): Res {
                return Res(
                    resId = resId,
                    memoryCacheEnabled = memoryCacheEnabled,
                    diskCacheEnabled = diskCacheEnabled
                )
            }
        }
    }

    /**
     * Image request to load an image file.
     */
    @Immutable
    class File(
        val file: java.io.File,
        memoryCacheEnabled: Boolean = true,
        diskCacheEnabled: Boolean = true,
    ) : ImageRequest(memoryCacheEnabled, diskCacheEnabled) {
        override fun toString(): String {
            return "File($file)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            other as File

            if (file != other.file) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + file.hashCode()
            return result
        }

        class Builder(private val file: java.io.File) : RequestBuilder<File>() {
            override fun build(): File {
                return File(
                    file = file,
                    memoryCacheEnabled = memoryCacheEnabled,
                    diskCacheEnabled = diskCacheEnabled
                )
            }
        }
    }

    /**
     * Image request will first try to fetch the image from the downloaded image directory, then
     * try to request image using the image framework.
     *
     * Only http urls are supported.
     */
    class Downloadable(
        val url: String,
        memoryCacheEnabled: Boolean = true,
        diskCacheEnabled: Boolean = true,
    ) : ImageRequest(memoryCacheEnabled, diskCacheEnabled) {
        init {
            require(url.startsWith("http://") || url.startsWith("https://")) {
                "The url must starts with 'http://' or 'https://'"
            }
        }

        override fun toString(): String {
            return "Downloadable($url)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            if (!super.equals(other)) return false

            other as Downloadable

            if (url != other.url) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + url.hashCode()
            return result
        }

        class Builder(private val url: String) : RequestBuilder<Downloadable>() {
            override fun build(): Downloadable {
                return Downloadable(
                    url = url,
                    memoryCacheEnabled = memoryCacheEnabled,
                    diskCacheEnabled = diskCacheEnabled
                )
            }
        }
    }
}
