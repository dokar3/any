package any.ui.barcode.camera.configure

import any.ui.barcode.camera.event.CameraEvent
import any.ui.barcode.camera.event.CameraEventDispatcher

internal class CameraConfigureScopeImpl(
    private val cameraEventDispatcher: CameraEventDispatcher
) : CameraConfigureScope {
    override fun onEvent(action: (CameraEvent) -> Unit) {
        cameraEventDispatcher.addListener(action)
    }
}