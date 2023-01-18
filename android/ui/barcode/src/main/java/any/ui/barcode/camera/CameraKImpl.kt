package any.ui.barcode.camera

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Size
import any.ui.barcode.camera.event.CameraEventListener
import any.ui.barcode.camera.scope.StartCameraScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CameraKImpl(
    private val context: Context,
    private val eventListener: CameraEventListener? = null,
) : CameraK {
    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    private var cameraManagerWrapper: CameraManagerWrapper? = null

    override suspend fun open(
        isFrontFacing: Boolean,
        imageFormat: Int,
        expectPreviewSize: Size,
        callback: StartCameraScope.() -> Unit
    ): Unit = withContext(Dispatchers.Default) {
        CameraManagerWrapper(
            cameraManager = cameraManager,
            eventListener = eventListener,
        ).let {
            callback(it)
            it.openCamera(
                isFrontFacing = isFrontFacing,
                imageFormat = imageFormat,
                expectPreviewSize = expectPreviewSize,
            )
            cameraManagerWrapper = it
        }
    }

    override suspend fun close(): Unit = withContext(Dispatchers.Default) {
        cameraManagerWrapper?.closeCamera()
    }

    override fun isOpened(): Boolean {
        return cameraManagerWrapper?.isOpened() == true
    }

    override fun turnOnTorch() {
        cameraManagerWrapper?.turnOnTorch()
    }

    override fun turnOffTorch() {
        cameraManagerWrapper?.turnOffTorch()
    }

    override fun toggleTorch() {
        if (isTorchOn()) {
            turnOffTorch()
        } else {
            turnOnTorch()
        }
    }

    override fun isTorchOn(): Boolean {
        return cameraManagerWrapper?.isTorchOn() == true
    }
}