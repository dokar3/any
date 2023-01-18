package any.ui.settings.privacy

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.foundation.layout.Column
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import any.base.prefs.isSecureScreenEnabled
import any.base.prefs.preferencesStore
import any.ui.common.widget.FlatSwitch
import any.ui.settings.SettingsItem
import any.ui.settings.SettingsItemIcon

@Composable
internal fun SecureScreenItem(
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val preferencesStore = context.preferencesStore()

    val isSecureScreenEnabled by preferencesStore.isSecureScreenEnabled
        .asStateFlow(scope)
        .collectAsState()

    fun changeSecureScreenEnabledState() {
        preferencesStore.isSecureScreenEnabled.value = !isSecureScreenEnabled
    }

    SettingsItem(
        modifier = modifier,
        icon = {
            SettingsItemIcon(painterResource(CommonUiR.drawable.ic_baseline_preview_24))
        },
        onClick = { changeSecureScreenEnabledState() },
        widget = {
            FlatSwitch(
                checked = isSecureScreenEnabled,
                onCheckedChange = { changeSecureScreenEnabledState() }
            )
        },
    ) {
        Column {
            Text(stringResource(BaseR.string.secure_screen))
            if (isSecureScreenEnabled) {
                Text(
                    text = stringResource(BaseR.string.secure_screen_on_summary),
                    fontSize = 13.sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                )
            }
        }
    }
}