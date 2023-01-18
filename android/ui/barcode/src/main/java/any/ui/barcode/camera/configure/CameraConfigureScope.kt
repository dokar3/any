package any.ui.barcode.camera.configure

import any.ui.barcode.camera.event.CameraEvent

interface CameraConfigureScope {
    fun onEvent(action: (CameraEvent) -> Unit)
}