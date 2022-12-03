package any.ui.barcode

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.TextureView
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import any.base.compose.StableHolder
import any.ui.barcode.camera.CameraK
import any.ui.barcode.camera.event.CameraEvent
import any.ui.barcode.camera.target.MLKitBarCodeScanning
import any.ui.barcode.camera.target.TextureViewPreview
import any.ui.barcode.camera.util.displayRotation
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.launch

class BarCodeScanningActivity : ComponentActivity() {
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
            startCamera()
        }

        override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        }

        override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            return true
        }

        override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
        }
    }

    private lateinit var textureView: TextureView

    private val camera = CameraK.create(this) {
        onEvent {
            when (it) {
                CameraEvent.CaptureConfigureFailed -> {
                    Log.e(TAG, "Camera capture configure failed")
                }
                CameraEvent.CaptureConfigured -> {
                    Log.d(TAG, "Camera capture configured")
                }
                CameraEvent.Disconnect -> {
                    Log.d(TAG, "Camera disconnected")
                }
                is CameraEvent.Error -> {
                    Log.e(TAG, "Camera error: ${it.error.message}")
                }
                CameraEvent.Opened -> {
                    Log.d(TAG, "Camera opened")
                }
                CameraEvent.NoImageTargets -> {
                    Log.d(TAG, "Camera no targets")
                }
                CameraEvent.Closed -> {
                    Log.d(TAG, "Camera closed")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        textureView = TextureView(this)
        textureView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        textureView.surfaceTextureListener = surfaceTextureListener

        setContent {
            CameraPreviewScreen(
                textureView = StableHolder(textureView),
                camera = StableHolder(camera),
            )
        }
    }

    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.CAMERA) != PERMISSION_GRANTED
        ) {
            requestCameraPermission()
        } else {
            startCamera()
        }
    }

    override fun onStop() {
        super.onStop()
        closeCamera()
    }

    override fun onDestroy() {
        super.onDestroy()
        textureView.surfaceTextureListener = null
    }

    private fun requestCameraPermission() {
        val launcher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startCamera()
            }
        }
        launcher.launch(Manifest.permission.CAMERA)
    }

    private fun startCamera() = lifecycleScope.launch {
        val size = Size(textureView.width, textureView.height)
        if (size.isZero()) return@launch
        camera.open(size) {
            onOpened {
                +TextureViewPreview(textureView)

                +MLKitBarCodeScanning(
                    displayRotation = displayRotation,
                    scannerOptions = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build(),
                ) {
                    setScanningResult(it)
                }
            }
        }
    }

    private fun closeCamera() = lifecycleScope.launch {
        camera.close()
    }

    private fun setScanningResult(text: String) {
        val result = Intent().apply {
            putExtra(RESULT_NAME, text)
        }
        setResult(RESULT_CODE, result)
        finish()
    }

    private fun Size.isZero(): Boolean {
        return width == 0 || height == 0
    }

    companion object {
        private const val TAG = "BarCodeActivity"

        const val RESULT_CODE = 1
        const val RESULT_NAME = "RESULT"
    }
}

@Composable
internal fun CameraPreviewScreen(
    textureView: StableHolder<TextureView>,
    camera: StableHolder<CameraK>,
    modifier: Modifier = Modifier,
) {
    var isTorchOn by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        AndroidView(
            factory = { textureView.value },
            modifier = Modifier.fillMaxWidth(),
        )

        val density = LocalDensity.current
        val navBarHeight = with(density) {
            WindowInsets.navigationBars.getBottom(density).toDp()
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(
                    start = 16.dp,
                    top = 0.dp,
                    end = 16.dp,
                    bottom = navBarHeight + 32.dp,
                ),
            horizontalArrangement = Arrangement.End,
        ) {
            IconButton(
                onClick = {
                    camera.value.toggleTorch()
                    isTorchOn = !isTorchOn
                },
                modifier = Modifier
                    .background(
                        color = Color.Black.copy(alpha = 0.2f),
                        shape = CircleShape,
                    )
            ) {
                val icon: Int
                val description: Int
                val tint: Color
                if (isTorchOn) {
                    icon = CommonUiR.drawable.ic_baseline_flash_on_24
                    description = BaseR.string.turn_on_flashlight
                    tint = Color.White
                } else {
                    icon = CommonUiR.drawable.ic_baseline_flash_off_24
                    description = BaseR.string.turn_off_flashlight
                    tint = Color.White.copy(alpha = 0.7f)
                }
                Icon(
                    painter = painterResource(icon),
                    contentDescription = stringResource(description),
                    tint = tint
                )
            }
        }
    }
}

