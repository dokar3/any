package com.dokar.any

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDestination
import androidx.navigation.compose.rememberNavController
import any.base.R
import any.base.compose.ImmutableHolder
import any.domain.entity.UpdatableService
import any.download.PostImageDownloader
import any.navigation.NavEvent
import any.navigation.NavigationBlocker
import any.navigation.Routes
import any.navigation.navigateAndReplace
import any.navigation.post
import any.ui.common.dialog.UpdateBuiltinServicesDialog
import any.ui.common.widget.SimpleDialog
import any.ui.imagepager.ImagePagerViewModel
import any.ui.service.ServiceDialog
import any.ui.settings.services.ServiceMgtViewModel
import any.ui.settings.services.ServicesUiState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

val MainScreenNavBlocker = NavigationBlocker()

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
    val navController = rememberNavController()

    val uiState by mainViewModel.uiState.collectAsState()
    val servicesUiState by serviceMgtViewModel.servicesUiState.collectAsState()

    var showExitDuringDownloadingDialog by remember { mutableStateOf(false) }

    fun isRouteBlocked(route: String): Boolean {
        if (MainScreenNavBlocker.isBlocked(route)) {
            return true
        }
        @SuppressLint("RestrictedApi")
        val request = NavDeepLinkRequest.Builder
            .fromUri(NavDestination.createRoute(route).toUri())
            .build()

        @SuppressLint("RestrictedApi")
        val rawRoute = navController.graph.matchDeepLink(request)
            ?.destination?.route ?: route
        return MainScreenNavBlocker.isBlocked(rawRoute)
    }

    fun navigate(event: NavEvent) {
        when (event) {
            NavEvent.Back -> {
                navController.popBackStack()
            }

            is NavEvent.Push -> {
                if (isRouteBlocked(event.route)) return
                navController.navigate(event.route)
            }

            is NavEvent.ReplaceWith -> {
                if (isRouteBlocked(event.route)) return
                navController.navigateAndReplace(event.route)
            }

            is NavEvent.PushImagePager -> {
                if (isRouteBlocked(event.route)) return
                imagePagerViewModel.images = event.images
                navController.navigate(event.route)
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
                .collect(::navigate)
        }
    }

    BackHandler {
        if (navController.previousBackStackEntry != null) {
            navController.popBackStack()
        } else {
            if (PostImageDownloader.get(context).hasDownloadingTasks()) {
                showExitDuringDownloadingDialog = true
            } else {
                (context as Activity).finish()
            }
        }
    }

    AnimatedAppNavGraph(
        onNavigate = ::navigate,
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
        onLoad = serviceMgtViewModel::loadUpdatableBuiltinServices,
        onIgnore = serviceMgtViewModel::ignoreBuiltinServicesUpdates,
        onUpdate = serviceMgtViewModel::updateBuiltinServices,
        onClearUpdatableServices = serviceMgtViewModel::resetBuiltinServicesUpdateState,
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
    onLoad: () -> Unit,
    onIgnore: () -> Unit,
    onUpdate: (List<UpdatableService>) -> Unit,
    onClearUpdatableServices: () -> Unit,
    servicesUiState: ServicesUiState,
) {
    rememberSaveable(inputs = emptyArray()) {
        onLoad()
        0
    }

    val updatableServices = servicesUiState.updatableBuiltinServices
    if (!updatableServices.isNullOrEmpty()) {
        UpdateBuiltinServicesDialog(
            onDismissRequest = onClearUpdatableServices,
            onUpdateClick = onUpdate,
            onIgnoreClick = onIgnore,
            updatableServices = ImmutableHolder(updatableServices),
            dismissOnClickOutside = false,
        )
    }
}
