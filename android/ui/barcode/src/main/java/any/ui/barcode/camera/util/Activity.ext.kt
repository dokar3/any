package any.ui.barcode.camera.util

import android.app.Activity
import android.os.Build

@Suppress("deprecation")
val Activity.displayRotation: Int
    get() {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation ?: windowManager.defaultDisplay.rotation
        } else {
            windowManager.defaultDisplay.rotation
        }
    }