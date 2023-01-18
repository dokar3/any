package any.ui.settings.privacy

import any.base.R as BaseR
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import any.ui.settings.SettingsViewModel

@Composable
fun PrivacySettings(
    viewModel: SettingsViewModel,
) {
    val res = LocalContext.current.resources
    LaunchedEffect(viewModel) {
        viewModel.updateTitle(res.getString(BaseR.string.privacy_settings))
        viewModel.setShowBackArrow(true)
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            AppPasswordItem()
        }

        item {
            SecureScreenItem()
        }
    }
}