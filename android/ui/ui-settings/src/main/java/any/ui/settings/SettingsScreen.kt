package any.ui.settings

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import any.base.prefs.preferencesStore
import any.base.prefs.showDevOptions
import any.base.util.compose.performLongPress
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.SettingsRoute
import any.navigation.navigateAndReplace
import any.ui.common.theme.sizes
import any.ui.common.widget.AnimatedBackButton
import any.ui.common.widget.NavigationBarSpacer
import any.ui.common.widget.SimpleTitleBar
import any.ui.common.widget.StatusBarSpacer
import any.ui.common.widget.TitleActionButton
import any.ui.settings.about.AboutScreen
import any.ui.settings.about.LibsScreen
import any.ui.settings.dev.DevSettings
import any.ui.settings.files.FilesAndDataSettings
import any.ui.settings.main.MainSettings
import any.ui.settings.menu.ActionMenuController
import any.ui.settings.privacy.PrivacySettings
import any.ui.settings.services.ServiceManagement
import any.ui.settings.ui.UiSettings
import any.ui.settings.viewmodel.SettingsViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onNavigate: (NavEvent) -> Unit,
    @SettingsRoute subSettings: String?,
    viewModel: SettingsViewModel = viewModel(),
) {
    val uiState by viewModel.settingsUiState.collectAsState()

    val settingsNavController = rememberNavController()

    val menuController = remember { ActionMenuController() }

    val onSettingsNavigate: (NavEvent) -> Unit = remember(settingsNavController) {
        {
            when (it) {
                NavEvent.Back -> settingsNavController.popBackStack()
                is NavEvent.Push -> settingsNavController.navigate(it.route)
                is NavEvent.PushImagePager -> TODO("Unsupported nav event in settings: $it")
                is NavEvent.ReplaceWith -> settingsNavController.navigateAndReplace(it.route)
            }
        }
    }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current!!.onBackPressedDispatcher

    Column(modifier = Modifier.fillMaxSize()) {
        StatusBarSpacer()

        SimpleTitleBar(
            height = MaterialTheme.sizes.titleBarHeight,
            startActionButton = {
                AnimatedBackButton(
                    visible = uiState.showBackButton,
                    onClick = { backDispatcher.onBackPressed() },
                )
            },
            endActionButton = {
                val item = menuController.currentItem
                if (item != null) {
                    TitleActionButton(
                        label = item.title,
                        onClick = { item.onClick?.invoke() },
                    ) {
                        Icon(
                            painter = painterResource(item.iconRes),
                            contentDescription = item.title,
                        )
                    }
                }
            },
        ) {
            val context = LocalContext.current
            val hapticFeedback = LocalHapticFeedback.current

            Text(
                text = uiState.title,
                modifier = Modifier.combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onLongClick = {
                        if (settingsNavController.currentBackStackEntry?.destination?.route
                            == Routes.Settings.MAIN
                        ) {
                            hapticFeedback.performLongPress()
                            context.preferencesStore().showDevOptions.let {
                                it.value = !it.value
                            }
                        }
                    },
                    onClick = {},
                ),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(weight = 1f, fill = false),
        ) {
            SettingsNavGraph(
                onRootNavigate = onNavigate,
                onSettingsNavigate = onSettingsNavigate,
                navController = settingsNavController,
                menuController = menuController,
                viewModel = viewModel,
                subSettings = subSettings,
            )
        }

        NavigationBarSpacer()
    }
}

@Composable
private fun SettingsNavGraph(
    onRootNavigate: (NavEvent) -> Unit,
    onSettingsNavigate: (NavEvent) -> Unit,
    navController: NavHostController,
    menuController: ActionMenuController,
    viewModel: SettingsViewModel,
    subSettings: String?,
) {
    NavHost(
        navController = navController,
        startDestination = subSettings?.ifEmpty { Routes.Settings.MAIN } ?: Routes.Settings.MAIN,
    ) {
        composable(Routes.Settings.MAIN) {
            MainSettings(onSettingsNavigate, viewModel)
        }

        composable(Routes.Settings.UI) {
            UiSettings(viewModel)
        }

        composable(Routes.Settings.SERVICE_MGT) {
            ServiceManagement(viewModel, menuController)
        }

        composable(Routes.Settings.FILES) {
            FilesAndDataSettings(viewModel)
        }

        composable(Routes.Settings.PRIVACY) {
            PrivacySettings(viewModel)
        }

        composable(Routes.Settings.ABOUT) {
            AboutScreen(onSettingsNavigate, viewModel)
        }

        composable(Routes.Settings.LIBS) {
            LibsScreen(viewModel)
        }

        composable(Routes.Settings.DEV) {
            DevSettings(
                onRootNavigate = onRootNavigate,
                menuController = menuController,
                viewModel = viewModel,
            )
        }
    }
}

