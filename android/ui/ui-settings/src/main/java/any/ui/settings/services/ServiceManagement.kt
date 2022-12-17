package any.ui.settings.services

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.FloatingActionButtonElevation
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import any.base.compose.ImmutableHolder
import any.base.compose.StableHolder
import any.base.model.UiMessage
import any.base.util.Dirs
import any.base.util.FileUtil
import any.base.util.compose.performLongPress
import any.base.util.isHttpUrl
import any.domain.entity.UiServiceManifest
import any.ui.common.dialog.UpdateBuiltinServicesDialog
import any.ui.common.modifier.fabOffset
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.widget.AnimatedPopup
import any.ui.common.widget.AnimatedPopupItem
import any.ui.common.widget.BasicDialog
import any.ui.common.widget.CheckableItem
import any.ui.common.widget.EditDialog
import any.ui.common.widget.FlatSwitch
import any.ui.common.widget.SimpleDialog
import any.ui.common.widget.UiMessagePopup
import any.ui.common.widget.rememberAnimatedPopupDismissRequester
import any.ui.service.ServiceDialog
import any.ui.settings.SettingsViewModel
import any.ui.settings.menu.ActionMenuController
import any.ui.settings.menu.ActionMenuItem
import com.dokar.sheets.BottomSheet
import com.dokar.sheets.PeekHeight
import com.dokar.sheets.rememberBottomSheetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ServiceManagement(
    settingsViewModel: SettingsViewModel,
    menuController: ActionMenuController,
    modifier: Modifier = Modifier,
    viewModel: ServiceMgtViewModel = viewModel(
        factory = ServiceMgtViewModel.Factory(LocalContext.current)
    ),
) {
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    val uiState by viewModel.servicesUiState.collectAsState()

    val addServicesSheetState = rememberBottomSheetState()

    val hapticFeedback = LocalHapticFeedback.current

    val isInSelection = uiState.isInSelection

    val servicesToConfigure: List<AppendableService> = uiState.servicesToConfigure

    var configuringService: AppendableService? by remember { mutableStateOf(null) }

    var showServicePickerDialog by remember { mutableStateOf(false) }

    var showRemoveAllSelectedServicesDialog by remember { mutableStateOf(false) }

    var showMoreMenu by remember { mutableStateOf(false) }

    val res = LocalContext.current.resources

    var showAddByLinkDialog by remember { mutableStateOf(false) }

    var isFabExpanded by remember { mutableStateOf(false) }

    var zipUri: Uri? by remember { mutableStateOf(null) }

    val pickZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { zipUri = it }
    )

    LaunchedEffect(viewModel) {
        settingsViewModel.setShowBackArrow(true)
        viewModel.loadDbServices()
    }

    LaunchedEffect(uiState, isInSelection, uiState.selectedServiceCount) {
        val count = if (isInSelection) {
            uiState.selectedServiceCount
        } else {
            uiState.services.size
        }
        settingsViewModel.updateTitle(res.getString(BaseR.string._services_with_count, count))
    }

    LaunchedEffect(zipUri) {
        if (zipUri == null) return@LaunchedEffect
        viewModel.viewModelScope.launch(Dispatchers.IO) {
            val zip = File(Dirs.servicesTempDir(context), "temp.zip")
            FileUtil.copyToFile(context, zipUri!!, zip)
            viewModel.loadServicesToConfigFromZip(zip = zip, deleteZipAfterAdding = true)
            zipUri = null
        }
    }

    LaunchedEffect(servicesToConfigure) {
        if (servicesToConfigure.size == 1) {
            // Show configuring dialog directly
            showServicePickerDialog = false
            configuringService = servicesToConfigure.first()
        } else if (servicesToConfigure.size > 1) {
            // Show the picker dialog
            showServicePickerDialog = true
            configuringService = null
        } else {
            showServicePickerDialog = false
            configuringService = null
        }
        showAddByLinkDialog = false
    }

    LaunchedEffect(uiState.isLoadingServiceToConfig) {
        if (!uiState.isLoadingServiceToConfig) {
            showAddByLinkDialog = false
        }
    }

    DisposableEffect(menuController) {
        menuController.pushItem(
            ActionMenuItem(
                title = res.getString(BaseR.string.more),
                iconRes = CommonUiR.drawable.ic_baseline_more_vert_24,
                onClick = { showMoreMenu = true },
            )
        )
        onDispose {
            menuController.popItem()
        }
    }

    BackHandler(enabled = isInSelection) {
        viewModel.unselectAllDbServices()
    }

    Box {
        val listState = rememberLazyListState()
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .verticalScrollBar(listState),
            state = listState,
            contentPadding = PaddingValues(bottom = 72.dp),
        ) {
            items(
                items = uiState.services,
                key = { it.id + it.name },
            ) { service ->
                val isSelected = uiState.selectedServices.contains(service)
                ServiceItem(
                    service = service,
                    isSelected = isSelected,
                    isInSelection = isInSelection,
                    onClick = {
                        if (isInSelection) {
                            if (isSelected) {
                                viewModel.unselectDbService(service)
                            } else {
                                viewModel.selectDbService(service)
                            }
                        } else {
                            viewModel.setServicesToConfigure(listOf(service))
                        }
                    },
                    onLongClick = {
                        hapticFeedback.performLongPress()
                        if (isInSelection) {
                            if (isSelected) {
                                viewModel.unselectDbService(service)
                            } else {
                                viewModel.selectDbService(service)
                            }
                        } else {
                            viewModel.selectDbService(service)
                        }
                    },
                    onEnabledChange = {
                        viewModel.updateDbService(service.copy(isEnabled = it))
                    },
                    modifier = Modifier.animateItemPlacement(),
                )
            }
        }

        val fabContainerModifier = if (isFabExpanded) {
            val background = MaterialTheme.colors.background
            val alpha = remember { Animatable(0f) }
            LaunchedEffect(alpha) {
                alpha.animateTo(0.9f)
            }
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawRect(color = background.copy(alpha = alpha.value))
                    drawContent()
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { isFabExpanded = false },
                )
                .fabOffset()
        } else {
            Modifier
                .align(Alignment.BottomEnd)
                .fabOffset()
        }

        val showError = uiState.message is UiMessage.Error
        val errorOffset by animateDpAsState(if (showError) (-56).dp else 0.dp)
        Column(
            modifier = fabContainerModifier
                .offset { IntOffset(0, errorOffset.roundToPx()) },
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Bottom,
        ) {
            AnimatedVisibility(
                visible = isFabExpanded,
                enter = slideInVertically { it / 2 } + fadeIn(),
                exit = slideOutVertically { it / 2 } + fadeOut(),
            ) {

                Column(horizontalAlignment = Alignment.End) {
                    LabeledFab(
                        onClick = {
                            showAddByLinkDialog = true
                            isFabExpanded = false
                        },
                        modifier = Modifier.padding(bottom = 16.dp),
                        backgroundColor = MaterialTheme.colors.secondary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = if (isFabExpanded) 6.dp else 0.dp,
                        ),
                        label = { Text(stringResource(BaseR.string.link)) },
                    ) {
                        Icon(
                            painter = painterResource(CommonUiR.drawable.ic_baseline_add_link_24),
                            contentDescription = stringResource(BaseR.string.add_from_network_link),
                            tint = Color.White,
                        )
                    }

                    LabeledFab(
                        onClick = {
                            pickZipLauncher.launch("application/zip")
                            isFabExpanded = false
                        },
                        modifier = Modifier.padding(bottom = 16.dp),
                        backgroundColor = MaterialTheme.colors.secondary,
                        elevation = FloatingActionButtonDefaults.elevation(
                            defaultElevation = if (isFabExpanded) 6.dp else 0.dp,
                        ),
                        label = { Text(stringResource(BaseR.string.zip)) },
                    ) {
                        Icon(
                            painter = painterResource(CommonUiR.drawable.ic_outline_zip_box),
                            contentDescription = stringResource(BaseR.string.add_from_local_file),
                            tint = Color.White,
                        )
                    }
                }
            }

            val fabRotation by animateFloatAsState(
                targetValue = if (isFabExpanded) -90f else 0f
            )
            LabeledFab(
                onClick = {
                    if (isInSelection) {
                        showRemoveAllSelectedServicesDialog = true
                    } else {
                        if (isFabExpanded) {
                            isFabExpanded = false
                            scope.launch {
                                addServicesSheetState.peek()
                            }
                        } else {
                            isFabExpanded = true
                        }
                    }
                },
                modifier = Modifier.graphicsLayer {
                    rotationZ = fabRotation
                },
                backgroundColor = if (isInSelection) {
                    MaterialTheme.colors.error
                } else {
                    MaterialTheme.colors.secondary
                },
                label = {
                    AnimatedVisibility(
                        visible = isFabExpanded,
                        enter = slideInHorizontally { it } + fadeIn(),
                        exit = slideOutHorizontally { it } + fadeOut(),
                    ) {
                        Text(stringResource(BaseR.string.builtin))
                    }
                },
            ) {
                Icon(
                    imageVector = if (isInSelection) Icons.Default.Delete else Icons.Default.Add,
                    contentDescription = stringResource(BaseR.string.add_builtin_services),
                    tint = Color.White,
                )
            }
        }

        if (showMoreMenu) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                val dismissRequester = rememberAnimatedPopupDismissRequester()
                AnimatedPopup(
                    dismissRequester = dismissRequester,
                    onDismissed = { showMoreMenu = false },
                    contentAlignmentToAnchor = Alignment.TopEnd,
                    offset = DpOffset((-16).dp, (-40).dp),
                    scaleAnimOrigin = TransformOrigin(1f, 0f),
                ) {
                    AnimatedPopupItem(
                        index = 0,
                        onClick = {
                            viewModel.loadUpdatableBuiltinServices(force = true)
                            dismissRequester.dismiss()
                        },
                        icon = {
                            Icon(
                                painter = painterResource(
                                    CommonUiR.drawable.ic_baseline_arrow_inward_24
                                ),
                                contentDescription = null
                            )
                        }
                    ) {
                        Text(stringResource(BaseR.string.update_builtin_services))
                    }
                }
            }
        }
    }

    BottomSheet(
        state = addServicesSheetState,
        peekHeight = PeekHeight.fraction(0.65f),
    ) {
        AddServices(
            viewModel = viewModel,
            modifier = Modifier.fillMaxHeight(),
        )
    }

    if (showAddByLinkDialog) {
        val isLoading = uiState.isLoadingServiceToConfig
        var url by remember { mutableStateOf("") }
        var error: String? by remember { mutableStateOf(null) }
        EditDialog(
            onDismissRequest = { showAddByLinkDialog = false },
            value = url,
            onValueChange = { url = it },
            title = { Text(stringResource(BaseR.string.add_from_network_link)) },
            placeholder = { Text("https://.../manifest.json") },
            label = error?.let { { Text(it) } },
            confirmText = { Text(stringResource(BaseR.string.confirm)) },
            cancelText = { Text(stringResource(android.R.string.cancel)) },
            isError = error != null,
            dismissOnClickOutside = !isLoading,
            dismissOnBackPress = !isLoading,
            dismissOnConfirm = false,
            cancelEnabled = !isLoading,
            confirmEnabled = !isLoading,
            onConfirmClick = {
                if (url.isHttpUrl()) {
                    viewModel.loadServiceToConfigByUrl(url)
                } else {
                    error = res.getString(BaseR.string.invalid_link)
                }
            },
        )
    }

    if (showServicePickerDialog && servicesToConfigure.isNotEmpty()) {
        var selectedService by remember { mutableStateOf(servicesToConfigure.first()) }
        BasicDialog(
            onDismissRequest = { showServicePickerDialog = false },
            title = { Text(stringResource(BaseR.string.choose_service)) },
            cancelText = { Text(stringResource(android.R.string.cancel)) },
            confirmText = { Text(stringResource(BaseR.string.next)) },
            onConfirmClick = { configuringService = selectedService },
        ) {
            ServiceSelector(
                services = StableHolder(servicesToConfigure),
                selected = selectedService,
                onSelect = { selectedService = it },
            )
        }
    }

    if (configuringService != null) {
        val appendableService = configuringService!!
        val service = appendableService.service
        val isAdded = appendableService.isAdded
        ServiceDialog(
            onDismissRequest = { viewModel.clearServicesToConfigure() },
            service = service,
            isAdded = isAdded,
            onSaveService = {
                viewModel.clearMessage()
                appendableService.onSaveService(it)
            },
        )
    }

    if (showRemoveAllSelectedServicesDialog) {
        SimpleDialog(
            onDismissRequest = { showRemoveAllSelectedServicesDialog = false },
            title = { Text(stringResource(BaseR.string.remove)) },
            text = { Text(stringResource(BaseR.string.remove_selected_services_alert)) },
            cancelText = { Text(stringResource(android.R.string.cancel)) },
            confirmText = {
                Text(
                    text = stringResource(BaseR.string.remove),
                    color = MaterialTheme.colors.error,
                )
            },
            onConfirmClick = { viewModel.removeSelectedDbServices() },
        )
    }

    val updatableBuiltinServices = uiState.updatableBuiltinServices
    if (updatableBuiltinServices != null) {
        UpdateBuiltinServicesDialog(
            onDismissRequest = viewModel::resetBuiltinServicesUpdateState,
            onUpdateClick = viewModel::updateBuiltinServices,
            updatableServices = ImmutableHolder(updatableBuiltinServices),
        )
    }

    UiMessagePopup(
        message = uiState.message,
        onClearMessage = { viewModel.clearMessage() },
        modifier = Modifier.height(48.dp),
        maxTextLines = 1,
        applyWindowInsetsToOffset = false,
    )
}

@Composable
private fun LabeledFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
    backgroundColor: Color = MaterialTheme.colors.secondary,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    label: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        label()

        Spacer(modifier = Modifier.width(8.dp))

        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            interactionSource = interactionSource,
            shape = shape,
            backgroundColor = backgroundColor,
            contentColor = contentColor,
            elevation = elevation,
            content = content
        )
    }
}

@Composable
private fun ServiceSelector(
    services: StableHolder<List<AppendableService>>,
    selected: AppendableService,
    onSelect: (AppendableService) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(services.value) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(it) }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = it.service.name,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = selected == it,
                    onClick = { onSelect(it) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ServiceItem(
    service: UiServiceManifest,
    isSelected: Boolean,
    isInSelection: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    CheckableItem(
        isChecked = isSelected,
        modifier = modifier
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        showCheckmark = isInSelection,
    ) {
        ServiceItem(service = service) {
            if (!isInSelection) {
                FlatSwitch(
                    checked = service.isEnabled,
                    onCheckedChange = onEnabledChange,
                )
            }
        }
    }
}
