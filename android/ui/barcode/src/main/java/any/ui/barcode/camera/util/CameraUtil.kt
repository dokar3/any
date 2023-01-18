package any.ui.barcode.camera.util

import android.graphics.Matrix
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import android.view.Surface
import android.view.TextureView
import kotlin.math.abs
import kotlin.math.min

internal object CameraUtil {
    fun getOptimalPreviewSize(
        sizes: Array<Size>,
        expectWidth: Int,
        expectHeight: Int,
        maxWidth: Int,
        maxHeight: Int,
    ): Size {
        require(sizes.isNotEmpty())
        val targetRatio = expectWidth.toFloat() / expectHeight
        val targetArea = expectWidth.toLong() * expectHeight
        return sizes
            .filter { it.width <= maxWidth && it.height <= maxHeight }
            .sortedBy {
                // Sort by aspect ratio
                val ratio = it.width.toFloat() / it.height
                abs(ratio - targetRatio)
            }
            .minByOrNull {
                // Choose the size which has closest area
                val area = it.width.toLong() * it.height
                abs(area - targetArea)
            }!!
    }

    // Based on from https://developers.google.com/ml-kit/vision/barcode-scanning/android
    /**
     * Get the angle by which an image must be rotated given the device's current
     * orientation.
     */
    @Throws(CameraAccessException::class)
    fun getImageRotationDegree(
        isFrontFacing: Boolean,
        characteristics: CameraCharacteristics,
        displayRotation: Int,
    ): Int {
        var rotationCompensation = when (displayRotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        // Get the device's sensor orientation.
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        rotationCompensation = if (isFrontFacing) {
            (sensorOrientation + rotationCompensation) % 360
        } else { // back-facing
            (sensorOrientation - rotationCompensation + 360) % 360
        }
        return rotationCompensation
    }

    // Copied from https://developer.android.com/training/camera2/camera-preview#textureview
    /**
     * This method calculates the transformation Matrix that we need to apply to the
     * TextureView to avoid a distorted preview.
     */
    fun computeTransformationMatrix(
        textureView: TextureView,
        characteristics: CameraCharacteristics,
        previewSize: Size,
        surfaceRotation: Int
    ): Matrix {
        val matrix = Matrix()

        val surfaceRotationDegrees = when (surfaceRotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }

        /* Rotation required to transform from the camera sensor orientation to the
         * device's current orientation in degrees. */
        val relativeRotation = computeRelativeRotation(characteristics, surfaceRotationDegrees)

        /* Scale factor required to scale the preview to its original size on the x-axis. */
        val scaleX =
            if (relativeRotation % 180 == 0) {
                textureView.width.toFloat() / previewSize.width
            } else {
                textureView.width.toFloat() / previewSize.height
            }
        /* Scale factor required to scale the preview to its original size on the y-axis. */
        val scaleY =
            if (relativeRotation % 180 == 0) {
                textureView.height.toFloat() / previewSize.height
            } else {
                textureView.height.toFloat() / previewSize.width
            }

        /* Scale factor required to fit the preview to the TextureView size. */
        // val finalScale = min(scaleX, scaleY)
        // Fill the TextureView
        val finalScale = min(scaleX, scaleY)

        /* The scale will be different if the buffer has been rotated. */
        if (relativeRotation % 180 == 0) {
            matrix.setScale(
                textureView.height / textureView.width.toFloat() / scaleY * finalScale,
                textureView.width / textureView.height.toFloat() / scaleX * finalScale,
                textureView.width / 2f,
                textureView.height / 2f
            )
        } else {
            matrix.setScale(
                1 / scaleX * finalScale,
                1 / scaleY * finalScale,
                textureView.width / 2f,
                textureView.height / 2f
            )
        }

        // Rotate the TextureView to compensate for the Surface's rotation.
        matrix.postRotate(
            -surfaceRotationDegrees.toFloat(),
            textureView.width / 2f,
            textureView.height / 2f
        )

        return matrix
    }

    // Copied from https://developer.android.com/training/camera2/camera-preview#relative-rotation
    /**
     * Computes rotation required to transform the camera sensor output orientation to the
     * device's current orientation in degrees.
     *
     * @param characteristics The CameraCharacteristics to query for the sensor orientation.
     * @param surfaceRotationDegrees The current device orientation as a Surface constant.
     * @return Relative rotation of the camera sensor output.
     */
    private fun computeRelativeRotation(
        characteristics: CameraCharacteristics,
        surfaceRotationDegrees: Int
    ): Int {
        val sensorOrientationDegrees =
            characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        // Reverse device orientation for back-facing cameras.
        val sign = if (characteristics.get(CameraCharacteristics.LENS_FACING) ==
            CameraCharacteristics.LENS_FACING_FRONT
        ) 1 else -1

        // Calculate desired orientation relative to camera orientation to make
        // the image upright relative to the device orientation.
        return (sensorOrientationDegrees - surfaceRotationDegrees * sign + 360) % 360
    }
}