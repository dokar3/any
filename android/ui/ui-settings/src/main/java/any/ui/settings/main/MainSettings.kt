package any.ui.settings.main

import any.base.R as BaseR
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import any.base.prefs.preferencesStore
import any.base.prefs.showDevOptions
import any.navigation.NavEvent
import any.ui.settings.SettingsViewModel

@Composable
internal fun MainSettings(
    onSettingsNavigate: (NavEvent) -> Unit,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current

    val scope = rememberCoroutineScope()

    val res = context.resources

    val showDevOptions by context.preferencesStore().showDevOptions
        .asStateFlow(scope)
        .collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.updateTitle(res.getString(BaseR.string.settings))
        viewModel.setShowBackArrow(true)
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        item {
            UiItem(onSettingsNavigate)
        }

        item {
            ServiceMgtItem(onSettingsNavigate)
        }

        item {
            FilesAndDataItem(onSettingsNavigate)
        }

        item {
            PrivacyItem(onSettingsNavigate)
        }

        if (showDevOptions) {
            item {
                DevItem(onSettingsNavigate)
            }
        }

        item {
            AboutItem(onSettingsNavigate)
        }
    }
}