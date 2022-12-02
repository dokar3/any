package com.dokar.any

import any.base.R as BaseR
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import any.base.R
import any.download.PostImageDownloader
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.navigateAndReplace
import any.navigation.post
import any.ui.common.widget.SimpleDialog
import any.ui.imagepager.ImagePagerViewModel
import any.ui.service.ServiceDialog
import any.ui.settings.services.viewmodel.ServiceMgtViewModel
import any.ui.settings.services.viewmodel.ServicesUiState
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    darkMode: Boolean,
    modifier: Modifier = Modifier,
    skipPassword: Boolean = false,
    imagePagerViewModel: ImagePagerViewModel = viewModel(),
    serviceMgtViewModel: ServiceMgtViewModel = viewModel(
        factory = ServiceMgtViewModel.Factory(LocalContext.current),
    ),
) {
    val context = LocalContext.current
    val navController = rememberAnimatedNavController()

    val uiState by mainViewModel.uiState.collectAsState()
    val servicesUiState by serviceMgtViewModel.servicesUiState.collectAsState()

    var showExitDuringDownloadingDialog by remember { mutableStateOf(false) }

    val onNavigate: (NavEvent) -> Unit = remember(navController, imagePagerViewModel) {
        {
            when (it) {
                NavEvent.Back -> {
                    navController.popBackStack()
                }

                is NavEvent.Push -> {
                    navController.navigate(it.route)
                }

                is NavEvent.ReplaceWith -> {
                    navController.navigateAndReplace(it.route)
                }

                is NavEvent.PushImagePager -> {
                    imagePagerViewModel.images = it.images
                    navController.navigate(it.route)
                }
            }
        }
    }

    LaunchedEffect(mainViewModel, navController) {
        launch {
            mainViewModel.uiState
                .mapNotNull { it.readingPostToNavigate }
                .collect {
                    val route = Routes.post(
                        url = it.url,
                        serviceId = it.serviceId,
                        initialElementIndex = it.elementIndex,
                        initialElementScrollOffset = it.elementScrollOffset,
                    )
                    navController.navigate(route)
                    mainViewModel.setReadingPostToNavigate(null)
                }
        }
        launch {
            mainViewModel.navEvent
                .distinctUntilChanged()
                .collect { onNavigate(it) }
        }
    }

    BackHandler {
        if (navController.backQueue.size > 2) {
            navController.popBackStack()
        } else if (PostImageDownloader.get(context).hasDownloadingTasks()) {
            showExitDuringDownloadingDialog = true
        } else {
            (context as Activity).finish()
        }
    }

    AnimatedAppNavGraph(
        onNavigate = onNavigate,
        onClearShortcutsDestination = { mainViewModel.setShortcutsDestination(null) },
        navController = navController,
        darkMode = darkMode,
        shortcutsDestination = uiState.shortcutsDestination,
        modifier = modifier,
        skipPassword = skipPassword,
    )

    if (showExitDuringDownloadingDialog) {
        SimpleDialog(
            onDismissRequest = { showExitDuringDownloadingDialog = false },
            title = { Text(stringResource(R.string.exit_app)) },
            text = { Text(stringResource(R.string.exit_app_during_downloading_alert)) },
            cancelText = { Text(stringResource(android.R.string.cancel)) },
            confirmText = { Text(stringResource(R.string.exit)) },
            onConfirmClick = {
                showExitDuringDownloadingDialog = false
                PostImageDownloader.get(context).cancelAll()
                (context as Activity).finish()
            },
        )
    }

    HandleConfiguringService(
        onLoadManifestById = { serviceMgtViewModel.loadServiceToConfigById(it) },
        serviceIdToConfigure = uiState.serviceIdToConfigure,
    )

    HandleAddingNewService(
        onLoadManifestByUrl = serviceMgtViewModel::loadServiceToConfigByUrl,
        onResetManifestUrl = { mainViewModel.setServiceManifestUrlToAdd(null) },
        manifestUrlToAdd = uiState.serviceManifestUrlToAdd,
        servicesUiState = servicesUiState,
    )

    HandleBuiltinServiceUpdates(
        onIgnore = { serviceMgtViewModel.ignoreBuiltinServicesUpdates() },
        onUpdate = { serviceMgtViewModel.updateBuiltinServices() },
        onResetUpdatableServiceCount = {
            serviceMgtViewModel.resetUpdatableBuiltinServiceCount()
        },
        servicesUiState = servicesUiState,
    )

    val serviceToConfigure = servicesUiState.servicesToConfigure.firstOrNull()
    if (serviceToConfigure != null) {
        ServiceDialog(
            onDismissRequest = {
                serviceMgtViewModel.clearServicesToConfigure()
                mainViewModel.setServiceIdToConfigure(null)
                mainViewModel.setServiceManifestUrlToAdd(null)
            },
            service = serviceToConfigure.service,
            isAdded = serviceToConfigure.isAdded,
            onSaveService = { serviceToConfigure.onSaveService(it) },
        )
    }
}

@Composable
private fun HandleConfiguringService(
    onLoadManifestById: (String) -> Unit,
    serviceIdToConfigure: String?,
) {
    LaunchedEffect(serviceIdToConfigure) {
        if (!serviceIdToConfigure.isNullOrEmpty()) {
            onLoadManifestById(serviceIdToConfigure)
        }
    }
}

@Composable
private fun HandleAddingNewService(
    onLoadManifestByUrl: (String) -> Unit,
    onResetManifestUrl: () -> Unit,
    manifestUrlToAdd: String?,
    servicesUiState: ServicesUiState,
) {
    LaunchedEffect(manifestUrlToAdd) {
        if (!manifestUrlToAdd.isNullOrEmpty()) {
            onLoadManifestByUrl(manifestUrlToAdd)
        }
    }

    if (servicesUiState.isLoadingServiceToConfig) {
        SimpleDialog(
            onDismissRequest = onResetManifestUrl,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            contentPadding = PaddingValues(16.dp),
        ) {
            Text(stringResource(R.string.loading_services))
        }
    }
}

@Composable
private fun HandleBuiltinServiceUpdates(
    onIgnore: () -> Unit,
    onUpdate: () -> Unit,
    onResetUpdatableServiceCount: () -> Unit,
    servicesUiState: ServicesUiState,
) {
    val updatableCount = servicesUiState.updatableBuiltinServiceCount
    val updatedCount = servicesUiState.updatedBuiltinServiceCount

    var showUpdatesAvailableDialog by remember { mutableStateOf(false) }
    var showUpdatedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(updatableCount) {
        if (updatableCount > 0) {
            showUpdatesAvailableDialog = true
        }
    }

    LaunchedEffect(updatedCount) {
        if (updatedCount >= 0) {
            showUpdatedDialog = true
        }
    }

    if (showUpdatesAvailableDialog) {
        SimpleDialog(
            onDismissRequest = {
                showUpdatesAvailableDialog = false
                onResetUpdatableServiceCount()
            },
            title = { Text(stringResource(BaseR.string.update_builtin_services)) },
            neutralText = { Text(stringResource(BaseR.string.ignore)) },
            cancelText = { Text(stringResource(android.R.string.cancel)) },
            confirmText = { Text(stringResource(BaseR.string.update)) },
            onNeutralClick = onIgnore,
            onConfirmClick = onUpdate,
            dismissOnClickOutside = false,
        ) {
            Text(stringResource(BaseR.string._builtin_services_update_info, updatableCount))
        }
    }

    if (showUpdatedDialog) {
        SimpleDialog(
            onDismissRequest = { showUpdatedDialog = false },
            title = { Text(stringResource(BaseR.string.update_builtin_services)) },
            confirmText = { Text(stringResource(android.R.string.ok)) },
        ) {
            Text(stringResource(BaseR.string._services_have_been_updated, updatedCount))
        }
    }
}
