package any.ui.settings.main

import any.base.R as BaseR
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.navPushEvent
import any.ui.settings.SettingsItem
import any.ui.settings.SettingsItemIcon

@Composable
internal fun AboutItem(
    onNavigate: (NavEvent) -> Unit,
) {
    SettingsItem(
        icon = { SettingsItemIcon(Icons.Default.Info) },
        onClick = { onNavigate(navPushEvent(Routes.Settings.ABOUT)) },
    ) {
        Text(stringResource(BaseR.string.about))
    }
}