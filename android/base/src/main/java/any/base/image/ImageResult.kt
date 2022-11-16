package any.base.image

sealed class ImageResult {
    class Bitmap(val value: android.graphics.Bitmap) : ImageResult()

    class File(val value: java.io.File): ImageResult()

    class InputStream(val value: java.io.InputStream): ImageResult()

    class Failure(val error: Throwable?) : ImageResult()
}
