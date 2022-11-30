package any.ui.settings.dev

import any.ui.common.R as CommonUiR
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import any.base.R
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.navPushEvent
import any.ui.settings.SettingsItem
import any.ui.settings.SettingsItemIcon
import any.ui.settings.menu.ActionMenuController
import any.ui.settings.viewmodel.SettingsViewModel

internal const val ROUTE_DEV_MAIN = "dev_main"

internal const val ROUTE_DEV_CRASH_LOGS = "dev_crash_logs"

internal const val ROUTE_DEV_JS_LOGS = "dev_js_logs"

internal const val ROUTE_BASELINE_PROFILE = "baseline_profiles"

@Composable
internal fun DevSettings(
    onRootNavigate: (NavEvent) -> Unit,
    menuController: ActionMenuController,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val currentNavController = rememberNavController()

    NavHost(
        navController = currentNavController,
        startDestination = ROUTE_DEV_MAIN,
        modifier = modifier,
    ) {
        composable(ROUTE_DEV_MAIN) {
            MainDevSettings(
                onRootNavigate = onRootNavigate,
                navController = currentNavController,
                viewModel = viewModel,
            )
        }

        composable(ROUTE_DEV_CRASH_LOGS) {
            CrashLogsScreen(
                viewModel = viewModel,
            )
        }

        composable(ROUTE_DEV_JS_LOGS) {
            JsLogsScreen(
                menuController = menuController,
                viewModel = viewModel,
            )
        }

        composable(ROUTE_BASELINE_PROFILE) {
            BaselineProfileScreen(viewModel = viewModel)
        }
    }
}

@Composable
private fun MainDevSettings(
    onRootNavigate: (NavEvent) -> Unit,
    navController: NavController,
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val resources = LocalContext.current.resources

    LaunchedEffect(viewModel) {
        viewModel.updateTitle(resources.getString(R.string.dev_settings))
        viewModel.setShowBackArrow(true)
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            SettingsItem(
                modifier = modifier,
                icon = {
                    SettingsItemIcon(
                        painterResource(CommonUiR.drawable.ic_baseline_text_snippet_24)
                    )
                },
                onClick = { navController.navigate(ROUTE_DEV_CRASH_LOGS) },
            ) {
                Text(stringResource(R.string.crash_logs))
            }
        }

        item {
            SettingsItem(
                modifier = modifier,
                icon = {
                    SettingsItemIcon(
                        painterResource(CommonUiR.drawable.ic_baseline_text_snippet_24)
                    )
                },
                onClick = { navController.navigate(ROUTE_DEV_JS_LOGS) },
            ) {
                Text(stringResource(R.string.js_logs))
            }
        }

        item {
            SettingsItem(
                modifier = modifier,
                icon = {
                    SettingsItemIcon(painterResource(CommonUiR.drawable.ic_baseline_textsms_24))
                },
                onClick = { onRootNavigate(navPushEvent(Routes.RUN_SQL)) },
            ) {
                Text(stringResource(R.string.run_sql))
            }
        }

        item {
            SettingsItem(
                modifier = modifier,
                icon = {
                    SettingsItemIcon(
                        painterResource(CommonUiR.drawable.ic_baseline_text_snippet_24)
                    )
                },
                onClick = { navController.navigate(ROUTE_BASELINE_PROFILE) },
            ) {
                Text(stringResource(R.string.baseline_profile))
            }
        }
    }
}