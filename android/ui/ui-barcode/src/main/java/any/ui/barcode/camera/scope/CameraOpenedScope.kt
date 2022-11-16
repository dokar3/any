package any.ui.barcode.camera.scope

import any.ui.barcode.camera.target.CameraImageTarget

interface CameraOpenedScope {
    fun addTarget(target: CameraImageTarget)

    operator fun CameraImageTarget.unaryPlus() {
        addTarget(this)
    }
}
