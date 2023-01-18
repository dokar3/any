package any.ui.barcode.camera

import android.annotation.SuppressLint
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.util.Log
import android.util.Size
import any.ui.barcode.camera.event.CameraEvent
import any.ui.barcode.camera.event.CameraEventListener
import any.ui.barcode.camera.event.CameraException
import any.ui.barcode.camera.scope.CameraOpenedScope
import any.ui.barcode.camera.scope.StartCameraScope
import any.ui.barcode.camera.target.CameraImageTarget
import any.ui.barcode.camera.util.AutoHandlerThread
import any.ui.barcode.camera.util.CameraUtil
import java.util.concurrent.Executors

internal class CameraManagerWrapper(
    private val cameraManager: CameraManager,
    private var eventListener: CameraEventListener?,
) : StartCameraScope {
    private val thread: AutoHandlerThread by lazy { AutoHandlerThread("CameraThread") }

    private val threadPool = Executors.newSingleThreadExecutor()

    private val cameraIds: Array<String> by lazy { cameraManager.cameraIdList }

    private val cameraOpenedScope = CameraOpenedScopeImpl()

    private var captureRequestBuilder: CaptureRequest.Builder? = null

    private var cameraDevice: CameraDevice? = null

    private var cameraInfo: CameraInfo? = null

    private var previewSize: Size? = null

    private var captureSession: CameraCaptureSession? = null

    private var onOpened: (CameraOpenedScope.() -> Unit)? = null
    private var onDisconnect: (() -> Unit)? = null
    private var onError: ((errorCode: Int) -> Unit)? = null

    private var isOpening = false
    private var isOpened = false

    private var isTorchOn = false

    private val cameraStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            isOpening = false
            isOpened = true
            eventListener?.onEvent(CameraEvent.Opened)
            cameraDevice = camera
            onOpened?.invoke(cameraOpenedScope)
            capture(cameraDevice = camera)
        }

        override fun onDisconnected(camera: CameraDevice) {
            isOpening = false
            eventListener?.onEvent(CameraEvent.Disconnect)
            onDisconnect?.invoke()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            isOpening = false
            val cameraError = when (error) {
                ERROR_CAMERA_DEVICE -> {
                    CameraException.CameraDevice()
                }
                ERROR_CAMERA_DISABLED -> {
                    CameraException.CameraDisabled()
                }
                ERROR_CAMERA_IN_USE -> {
                    CameraException.CameraInUse()
                }
                ERROR_CAMERA_SERVICE -> {
                    CameraException.CameraService()
                }
                ERROR_MAX_CAMERAS_IN_USE -> {
                    CameraException.MaxCameraInUse()
                }
                else -> {
                    CameraException.Unknown()
                }
            }
            eventListener?.onEvent(CameraEvent.Error(error = cameraError))
            onError?.invoke(error)
        }

        override fun onClosed(camera: CameraDevice) {
            super.onClosed(camera)
            eventListener?.onEvent(CameraEvent.Closed)
            eventListener = null
            stopBackgroundThread()
        }
    }

    private val captureStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(session: CameraCaptureSession) {
            captureSession = session
            eventListener?.onEvent(CameraEvent.CaptureConfigured)
            captureRequestBuilder?.let {
                session.setRepeatingRequest(it.build(), null, thread.handler)
            }
        }

        override fun onConfigureFailed(session: CameraCaptureSession) {
            eventListener?.onEvent(CameraEvent.CaptureConfigureFailed)
        }
    }

    override fun onOpened(action: CameraOpenedScope.() -> Unit) {
        this.onOpened = action
    }

    override fun onDisconnect(action: () -> Unit) {
        this.onDisconnect = action
    }

    override fun onError(action: (errorCode: Int) -> Unit) {
        this.onError = action
    }

    @SuppressLint("MissingPermission")
    fun openCamera(
        isFrontFacing: Boolean,
        imageFormat: Int,
        expectPreviewSize: Size,
    ) {
        if (isOpening) return
        isOpened = true
        try {
            val cameraInfo = getTargetCameraInfo(isFrontFacing = isFrontFacing)
            this.cameraInfo = cameraInfo
            this.previewSize = getPreviewSize(
                cameraInfo = cameraInfo,
                imageFormat = imageFormat,
                expectPreviewSize = expectPreviewSize,
            ).also {
                Log.d("TAG", "openCamera: preview size: $it")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                cameraManager.openCamera(cameraInfo.id, threadPool, cameraStateCallback)
            } else {
                cameraManager.openCamera(cameraInfo.id, cameraStateCallback, thread.handler)
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
            eventListener?.onEvent(CameraEvent.Error(CameraException.CameraDisabled()))
        }
    }

    private fun getTargetCameraInfo(isFrontFacing: Boolean): CameraInfo {
        return cameraIds.asSequence()
            .map {
                CameraInfo(
                    id = it,
                    isFrontFacing = isFrontFacing,
                    characteristics = cameraManager.getCameraCharacteristics(it),
                )
            }
            .first {
                if (isFrontFacing) {
                    it.characteristics.isFrontLens()
                } else {
                    it.characteristics.isBackLens()
                }
            }
    }

    private fun getPreviewSize(
        cameraInfo: CameraInfo,
        imageFormat: Int,
        expectPreviewSize: Size,
    ): Size {
        val previewSizes = cameraInfo.characteristics
            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            .getOutputSizes(imageFormat)
        return CameraUtil.getOptimalPreviewSize(
            sizes = previewSizes,
            expectWidth = expectPreviewSize.width,
            expectHeight = expectPreviewSize.height,
            maxWidth = 3000,
            maxHeight = 3000,
        )
    }

    fun closeCamera() {
        isOpened = false
        onOpened = null
        onError = null
        onDisconnect = null
        cameraDevice?.close()
        cameraOpenedScope.closeAndClearTargets()
    }

    fun isOpened(): Boolean {
        return isOpened
    }

    fun turnOnTorch() {
        toggleTorch(true)
    }

    fun turnOffTorch() {
        toggleTorch(false)
    }

    fun isTorchOn(): Boolean {
        return isTorchOn
    }

    private fun toggleTorch(on: Boolean) {
        if (!isOpened) return
        if (cameraInfo?.isFrontFacing == true) return
        val captureBuilder = this.captureRequestBuilder ?: return
        val captureSession = this.captureSession ?: return
        if (on) {
            captureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH)
        } else {
            captureBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF)
        }
        captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        captureSession.setRepeatingRequest(captureBuilder.build(), null, thread.handler)
        isTorchOn = on
    }

    private fun stopBackgroundThread() {
        threadPool.shutdownNow()
        thread.quitSafely()
    }

    @Suppress("deprecation")
    private fun capture(cameraDevice: CameraDevice) {
        val targets = cameraOpenedScope.getTargets()
        if (targets.isEmpty()) {
            eventListener?.onEvent(CameraEvent.NoImageTargets)
            captureRequestBuilder = null
            return
        }
        val captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        val size = checkNotNull(previewSize)
        val surfaces = targets.map {
            it.getSurface(
                cameraInfo = checkNotNull(cameraInfo),
                width = size.width,
                height = size.height,
            )
        }
        surfaces.forEach { captureBuilder.addTarget(it) }

        captureRequestBuilder = captureBuilder

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val output = surfaces.map { OutputConfiguration(it) }
            val sessionConfig = SessionConfiguration(
                SessionConfiguration.SESSION_REGULAR,
                output,
                threadPool,
                captureStateCallback
            )
            cameraDevice.createCaptureSession(sessionConfig)
        } else {
            cameraDevice.createCaptureSession(
                surfaces,
                captureStateCallback,
                thread.handler
            )
        }
    }

    private fun CameraCharacteristics.isBackLens(): Boolean {
        return get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
    }

    private fun CameraCharacteristics.isFrontLens(): Boolean {
        return get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
    }

    class CameraOpenedScopeImpl : CameraOpenedScope {
        private val targets = mutableListOf<CameraImageTarget>()

        override fun addTarget(target: CameraImageTarget) {
            targets.add(target)
        }

        fun getTargets(): List<CameraImageTarget> = targets.toList()

        fun closeAndClearTargets() {
            targets.forEach { it.close() }
            targets.clear()
        }
    }
}











