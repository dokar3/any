package any.ui.settings.privacy

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import any.base.prefs.appPassword
import any.base.prefs.preferencesStore
import any.ui.common.widget.EditDialog
import any.ui.common.widget.FlatSwitch
import any.ui.settings.SettingsItem
import any.ui.settings.SettingsItemIcon

@Composable
internal fun AppPasswordItem(
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val preferencesStore = context.preferencesStore()

    val appPassword by preferencesStore.appPassword
        .asStateFlow(scope)
        .collectAsState()

    val appPasswordEnabled = !appPassword.isNullOrEmpty()

    var passwordDialogType by remember { mutableStateOf(PasswordDialogType.None) }

    fun changePasswordState() {
        passwordDialogType = if (appPasswordEnabled) {
            PasswordDialogType.ClearPassword
        } else {
            PasswordDialogType.SetPassword
        }
    }

    SettingsItem(
        modifier = modifier,
        icon = {
            SettingsItemIcon(painterResource(CommonUiR.drawable.ic_baseline_privacy_tip_24))
        },
        onClick = { changePasswordState() },
        widget = {
            FlatSwitch(
                checked = appPasswordEnabled,
                onCheckedChange = { changePasswordState() }
            )
        },
    ) {
        Text(stringResource(BaseR.string.app_password))
    }

    if (passwordDialogType != PasswordDialogType.None) {
        val type = passwordDialogType
        val resources = LocalContext.current.resources
        var password by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        EditDialog(
            onDismissRequest = { passwordDialogType = PasswordDialogType.None },
            value = password,
            onValueChange = { text ->
                password = text.filter { it.isDigit() }
                error = null
            },
            title = {
                when (type) {
                    PasswordDialogType.ClearPassword -> {
                        Text(stringResource(BaseR.string.clear_password))
                    }

                    PasswordDialogType.SetPassword -> {
                        Text(stringResource(BaseR.string.set_password))
                    }

                    PasswordDialogType.None -> throw IllegalStateException("!")
                }
            },
            dismissOnConfirm = false,
            onConfirmClick = {
                when (type) {
                    PasswordDialogType.ClearPassword -> {
                        if (preferencesStore.appPassword.value == password) {
                            preferencesStore.appPassword.value = null
                            passwordDialogType = PasswordDialogType.None
                        } else {
                            error = resources.getString(BaseR.string.incorrect_password)
                        }
                    }

                    PasswordDialogType.SetPassword -> {
                        preferencesStore.appPassword.value = password
                        passwordDialogType = PasswordDialogType.None
                    }

                    else -> {}
                }
            },
            keyboardType = KeyboardType.NumberPassword,
            isError = !error.isNullOrEmpty(),
            label = if (error != null) {
                {
                    Text(text = error!!)
                }
            } else {
                null
            },
        )
    }
}

private enum class PasswordDialogType {
    None,
    ClearPassword,
    SetPassword,
}
