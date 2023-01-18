package any.ui.barcode.camera.event

sealed class CameraEvent {
    object Opened : CameraEvent()

    object Closed : CameraEvent()

    object Disconnect : CameraEvent()

    class Error(val error: CameraException) : CameraEvent()

    object CaptureConfigured : CameraEvent()

    object CaptureConfigureFailed : CameraEvent()

    object NoImageTargets : CameraEvent()
}
