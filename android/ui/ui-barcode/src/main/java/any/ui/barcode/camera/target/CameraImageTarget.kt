package any.ui.barcode.camera.target

import android.view.Surface
import any.ui.barcode.camera.CameraInfo

interface CameraImageTarget {
    fun getSurface(cameraInfo: CameraInfo, width: Int, height: Int): Surface

    fun close()
}