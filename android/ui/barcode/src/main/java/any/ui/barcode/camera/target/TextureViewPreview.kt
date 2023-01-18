package any.ui.barcode.camera.target

import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import any.ui.barcode.camera.CameraInfo
import any.ui.barcode.camera.util.CameraUtil

class TextureViewPreview(
    private val view: TextureView,
) : CameraImageTarget {
    override fun getSurface(
        cameraInfo: CameraInfo,
        width: Int,
        height: Int
    ): Surface {
        val matrix = CameraUtil.computeTransformationMatrix(
            textureView = view,
            characteristics = cameraInfo.characteristics,
            previewSize = Size(width, height),
            surfaceRotation = view.display.rotation,
        )
        Log.d("TAG", "PREVIEW getSurface: width: $width, height: $height, matrix:\n$matrix")
        view.setTransform(matrix)
        view.surfaceTexture?.setDefaultBufferSize(width, height)
        return Surface(view.surfaceTexture)
    }

    override fun close() {
        view.surfaceTexture?.release()
    }
}