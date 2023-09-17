package any.ui.settings.files

import android.R
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import any.ui.common.dialog.SimpleDialog
import any.ui.settings.SettingsItem
import any.ui.settings.SettingsItemIcon
import any.base.R as BaseR
import any.ui.common.R as CommonUiR

@Composable
internal fun CleanUpDbItem(
    onRequestCleanDb: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCleanDbDialog by remember { mutableStateOf(false) }

    SettingsItem(
        modifier = modifier,
        icon = {
            SettingsItemIcon(
                painter = painterResource(CommonUiR.drawable.ic_baseline_cleaning_services_24),
            )
        },
        onClick = { showCleanDbDialog = true },
    ) {
        Text(stringResource(BaseR.string.clean_up_database))
    }

    if (showCleanDbDialog) {
        SimpleDialog(
            onDismissRequest = { showCleanDbDialog = false },
            title = { Text(stringResource(BaseR.string.clean_up_database)) },
            text = { Text(stringResource(BaseR.string.clean_up_database_alert)) },
            cancelText = { Text(stringResource(R.string.cancel)) },
            confirmText = {
                Text(
                    text = stringResource(BaseR.string.clean),
                    color = MaterialTheme.colors.error,
                )
            },
            onConfirmClick = onRequestCleanDb,
        )
    }
}