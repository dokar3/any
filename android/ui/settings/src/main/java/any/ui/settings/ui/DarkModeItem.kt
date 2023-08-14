package any.ui.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import any.base.model.DarkMode
import any.base.prefs.darkMode
import any.base.prefs.darkModeFlow
import any.base.prefs.preferencesStore
import any.ui.settings.SettingsItem
import any.ui.settings.SettingsItemIcon
import any.base.R as BaseR
import any.ui.common.R as CommonUiR

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DarkModeItem(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val preferencesStore = context.preferencesStore()

    val darkMode by preferencesStore.darkModeFlow()
        .collectAsState(initial = preferencesStore.darkMode)

    val onDarkModeChanged: (DarkMode) -> Unit = remember {
        { mode ->
            preferencesStore.darkMode = mode
        }
    }

    SettingsItem(
        modifier = modifier,
        icon = {
            SettingsItemIcon(painterResource(CommonUiR.drawable.ic_baseline_nights_stay_24))
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(BaseR.string.dark_mode))

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CheckableButton(
                    title = stringResource(BaseR.string.dark_mode_on),
                    checked = darkMode == DarkMode.Yes,
                    onCheckedChange = { onDarkModeChanged(DarkMode.Yes) },
                )

                CheckableButton(
                    title = stringResource(BaseR.string.dark_mode_off),
                    checked = darkMode == DarkMode.No,
                    onCheckedChange = { onDarkModeChanged(DarkMode.No) },
                )

                CheckableButton(
                    title = stringResource(BaseR.string.system_default),
                    checked = darkMode == DarkMode.System,
                    onCheckedChange = { onDarkModeChanged(DarkMode.System) },
                )
            }
        }
    }
}
