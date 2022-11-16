package any.ui.barcode.camera

import android.content.Context
import android.graphics.ImageFormat
import android.util.Size
import any.ui.barcode.camera.configure.CameraConfigureScope
import any.ui.barcode.camera.configure.CameraConfigureScopeImpl
import any.ui.barcode.camera.event.CameraEventDispatcher
import any.ui.barcode.camera.scope.StartCameraScope

interface CameraK {
    suspend fun open(
        isFrontFacing: Boolean,
        imageFormat: Int,
        expectPreviewSize: Size,
        callback: StartCameraScope.() -> Unit
    )

    suspend fun open(
        expectPreviewSize: Size,
        callback: StartCameraScope.() -> Unit,
    ) {
        open(
            isFrontFacing = false,
            imageFormat = ImageFormat.YUV_420_888,
            expectPreviewSize = expectPreviewSize,
            callback = callback,
        )
    }

    suspend fun close()

    fun isOpened(): Boolean

    fun turnOnTorch()

    fun turnOffTorch()

    fun toggleTorch()

    fun isTorchOn(): Boolean

    companion object {
        fun create(
            context: Context,
            config: (CameraConfigureScope.() -> Unit)? = null,
        ): CameraK {
            val eventDispatcher = if (config != null) {
                CameraEventDispatcher().also {
                    CameraConfigureScopeImpl(cameraEventDispatcher = it).run(config)
                }
            } else {
                null
            }
            return CameraKImpl(context, eventDispatcher)
        }
    }
}