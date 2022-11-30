package any.ui.settings.dev

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import any.base.util.Permissions
import any.ui.jslogger.FloatingLoggerService
import any.ui.jslogger.LoggerScreen
import any.ui.settings.menu.ActionMenuController
import any.ui.settings.menu.ActionMenuItem
import any.ui.settings.viewmodel.SettingsViewModel

@Composable
internal fun JsLogsScreen(
    menuController: ActionMenuController,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val resources = LocalContext.current.resources

    LaunchedEffect(viewModel) {
        viewModel.updateTitle(resources.getString(BaseR.string.js_logs))
        viewModel.setShowBackArrow(true)
    }

    DisposableEffect(menuController) {
        menuController.pushItem(
            ActionMenuItem(
                title = resources.getString(BaseR.string.floating),
                iconRes = CommonUiR.drawable.ic_baseline_layers_24,
                onClick = {
                    val activity = context as Activity
                    if (Permissions.checkOrRequestFloatingPermission(activity)) {
                        FloatingLoggerService.show(context)
                    }
                },
            )
        )
        onDispose {
            menuController.popItem()
        }
    }
    LaunchedEffect(menuController) {
    }

    LoggerScreen(modifier = modifier)
}