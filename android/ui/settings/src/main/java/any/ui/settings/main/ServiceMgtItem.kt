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
internal fun ServiceMgtItem(
    onNavigate: (NavEvent) -> Unit,
) {
    SettingsItem(
        icon = {
            SettingsItemIcon(painterResource(CommonUiR.drawable.ic_app_filled))
        },
        onClick = {
            onNavigate(navPushEvent(Routes.Settings.SERVICE_MGT))
        }
    ) {
        Text(stringResource(BaseR.string.service_management))
    }
}