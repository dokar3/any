package any.ui.barcode.camera.target

import android.graphics.ImageFormat
import android.media.Image
import any.ui.barcode.camera.util.CameraUtil
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class MLKitBarCodeScanning(
    private val displayRotation: Int,
    scannerOptions: BarcodeScannerOptions,
    private val onResult: (String) -> Unit,
) : ImageReaderTarget(
    format = ImageFormat.YUV_420_888,
    maxImages = 2,
) {
    private val scanner = BarcodeScanning.getClient(scannerOptions)

    private var isProcessing = false

    private var isClosed = false

    override fun shouldAcceptNextImage(): Boolean {
        return !isProcessing && !isClosed
    }

    override fun onImageAvailable(image: Image) {
        if (isClosed) return
        val cameraInfo = this.cameraInfo ?: return
        isProcessing = true

        val rotationDegree = CameraUtil.getImageRotationDegree(
            isFrontFacing = cameraInfo.isFrontFacing,
            characteristics = cameraInfo.characteristics,
            displayRotation = displayRotation,
        )

        val inputImage = InputImage.fromMediaImage(image, rotationDegree)

        scanner.process(inputImage)
            .addOnSuccessListener {
                val barCode = it.firstOrNull() ?: return@addOnSuccessListener
                barCode.rawValue?.let(onResult)
            }
            .addOnCompleteListener {
                image.close()
                isProcessing = false
            }
    }

    override fun close() {
        super.close()
        scanner.close()
        isClosed = true
    }
}