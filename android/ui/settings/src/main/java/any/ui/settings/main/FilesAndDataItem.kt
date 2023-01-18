package any.ui.settings.main

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.navPushEvent
import any.ui.settings.SettingsItem
import any.ui.settings.SettingsItemIcon

@Composable
internal fun FilesAndDataItem(
    onNavigate: (NavEvent) -> Unit,
) {
    SettingsItem(
        icon = {
            SettingsItemIcon(painterResource(CommonUiR.drawable.ic_baseline_storage_24))
        },
        onClick = {
            onNavigate(navPushEvent(Routes.Settings.FILES))
        }
    ) {
        Text(stringResource(BaseR.string.files_and_data))
    }
}