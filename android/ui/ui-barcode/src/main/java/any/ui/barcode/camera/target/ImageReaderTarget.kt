package any.ui.barcode.camera.target

import android.graphics.ImageFormat
import android.media.Image
import android.media.ImageReader
import android.view.Surface
import any.ui.barcode.camera.util.AutoHandlerThread
import any.ui.barcode.camera.CameraInfo

abstract class ImageReaderTarget(
    private val format: Int = ImageFormat.JPEG,
    private val maxImages: Int = 1,
) : CameraImageTarget {
    private var imageReader: ImageReader? = null

    private val thread: AutoHandlerThread by lazy { AutoHandlerThread("ImageReaderThread") }

    protected var cameraInfo: CameraInfo? = null

    abstract fun shouldAcceptNextImage(): Boolean

    abstract fun onImageAvailable(image: Image)

    override fun getSurface(
        cameraInfo: CameraInfo,
        width: Int,
        height: Int
    ): Surface {
        this.cameraInfo = cameraInfo
        return ImageReader.newInstance(width, height, format, maxImages).also { ir ->
            imageReader = ir
            ir.setOnImageAvailableListener(
                {
                    if (shouldAcceptNextImage()) {
                        onImageAvailable(it.acquireLatestImage())
                    }
                },
                thread.handler,
            )
        }.surface
    }

    override fun close() {
        imageReader?.close()
        thread.quitSafely()
        thread.join()
    }
}