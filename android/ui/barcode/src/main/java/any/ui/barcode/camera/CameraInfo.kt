package any.ui.barcode.camera

import android.hardware.camera2.CameraCharacteristics

data class CameraInfo(
    val id: String,
    val isFrontFacing: Boolean,
    val characteristics: CameraCharacteristics,
)