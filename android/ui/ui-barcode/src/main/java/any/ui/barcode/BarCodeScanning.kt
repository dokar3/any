package any.ui.barcode

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun BarCodeScanning(
    onResult: (String?) -> Unit,
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == BarCodeScanningActivity.RESULT_CODE) {
            onResult(it.data?.getStringExtra(BarCodeScanningActivity.RESULT_NAME))
        } else {
            onResult(null)
        }
    }
    LaunchedEffect(launcher) {
        val intent = Intent(context, BarCodeScanningActivity::class.java)
        launcher.launch(intent)
    }
}