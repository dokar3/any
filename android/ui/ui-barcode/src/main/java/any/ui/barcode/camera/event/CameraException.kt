package any.ui.barcode.camera.event

sealed class CameraException(override val message: String?) : Exception(message) {
    class CameraInUse(message: String? = "Camera in use") : CameraException(message)

    class MaxCameraInUse(message: String? = "Max camera in use") : CameraException(message)

    class CameraDisabled(message: String? = "Camera disabled") : CameraException(message)

    class CameraDevice(message: String? = "Camera device error") : CameraException(message)

    class CameraService(message: String? = "Camera service error") : CameraException(message)

    class Unknown(message: String? = "Unknown error") : CameraException(message)
}