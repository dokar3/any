package any.ui.service

import android.R
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Button
import androidx.compose.material.ContentAlpha
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import any.base.compose.ImmutableHolder
import any.base.compose.StableHolder
import any.base.image.ImageLoader
import any.base.image.ImageRequest
import any.base.image.ImageState
import any.base.result.ValidationResult
import any.base.util.Intents
import any.data.entity.CookiesUaValue
import any.data.entity.PostsViewType
import any.data.entity.ServiceConfig
import any.data.entity.ServiceManifest
import any.data.entity.ServiceResource
import any.data.entity.value
import any.data.js.ServiceApiVersion
import any.domain.entity.UiServiceManifest
import any.richtext.RichContent
import any.ui.common.dialog.BasicDialog
import any.ui.common.image.AsyncImage
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.richtext.Html
import any.ui.common.richtext.RichText
import any.ui.common.theme.secondaryText
import any.ui.common.widget.DoubleEndDashedDivider
import any.ui.common.widget.ErrorMessage
import any.ui.common.widget.FlatSwitch
import any.ui.common.widget.ProgressBar
import any.ui.common.widget.WarningMessage
import com.dokar.sheets.rememberBottomSheetState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import any.base.R as BaseR
import any.ui.common.R as CommonUiR

@Composable
fun ServiceDialog(
    onDismissRequest: () -> Unit,
    service: UiServiceManifest,
    isAdded: Boolean,
    modifier: Modifier = Modifier,
    onSaveService: suspend (toSave: ServiceManifest) -> ServiceManifest? = { it },
    viewModel: ServiceViewModel = viewModel(
        factory = ServiceViewModel.Factory(LocalContext.current)
    ),
) {
    val scope = rememberCoroutineScope()

    val res = LocalContext.current.resources

    val uiState by viewModel.serviceUiState.collectAsState()

    val isValidating = uiState.isValidating
    val validations = uiState.validations

    var serviceNameError: String? by remember { mutableStateOf(null) }
    var serviceName by remember(service.name) { mutableStateOf(service.name) }

    var viewType by remember(service) { mutableStateOf(service.postsViewType) }

    var runValidator by remember(service.forceConfigsValidation, isAdded) {
        val force = service.forceConfigsValidation ?: false
        mutableStateOf(force && !isAdded)
    }

    var serviceChanged by remember { mutableStateOf(false) }

    val requiredFields = remember(service.configs) {
        service.configs?.filter { it.visibleToUser && it.required }
    }

    val optionalFields = remember(service.configs) {
        service.configs?.filter { it.visibleToUser && !it.required }
    }

    val invisibleFields = remember(service.configs) {
        service.configs?.filterNot { it.visibleToUser }
    }

    val values = remember(service.configs) {
        service.configs?.associate { it.key to it.value }?.toMutableMap() ?: mutableMapOf()
    }

    val serviceDetailsSheet = rememberBottomSheetState()

    LaunchedEffect(viewModel, service) {
        viewModel.checkUpgradeInfo(service)
    }

    LaunchedEffect(uiState.serviceToSave) {
        uiState.serviceToSave?.let {
            val updated = onSaveService(it)
            if (updated != null) {
                viewModel.saveService(updated)
            } else {
                onDismissRequest()
            }
        }
    }

    LaunchedEffect(uiState.savedService) {
        if (uiState.savedService != null) {
            onDismissRequest()
        }
    }

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.resetUiState()
        }
    }

    BasicDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.heightIn(max = 600.dp),
        title = {
            val upgradeInfo = uiState.updateInfo
            if (upgradeInfo != null) {
                Column {
                    Text(stringResource(BaseR.string.upgrade_service))

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${upgradeInfo.fromVersion} → ${upgradeInfo.toVersion}",
                        color = MaterialTheme.colors.secondaryText,
                        fontSize = 14.sp,
                    )
                }
            } else if (isAdded) {
                Text(stringResource(BaseR.string.update_service))
            } else {
                Text(stringResource(BaseR.string.add_service))
            }
        },
        dismissOnClickOutside = !isValidating,
        dismissOnBackPress = !isValidating,
        dismissOnConfirm = false,
        neutralEnabled = false,
        cancelEnabled = !isValidating,
        confirmEnabled = !isValidating,
        neutralText = {
            if (isValidating) {
                ProgressBar(size = 20.dp)
            }
        },
        cancelText = { Text(stringResource(R.string.cancel)) },
        confirmText = {
            Text(
                text = if (uiState.updateInfo != null) {
                    stringResource(BaseR.string.upgrade)
                } else if (isAdded) {
                    stringResource(BaseR.string.update)
                } else {
                    stringResource(BaseR.string.add)
                },
                fontWeight = if (isAdded && serviceChanged) {
                    FontWeight.Bold
                } else {
                    LocalTextStyle.current.fontWeight
                },
            )
        },
        onConfirmClick = {
            if (serviceName.isNotEmpty()) {
                viewModel.tryValidateConfigsAndSave(
                    service = service.copy(name = serviceName, postsViewType = viewType),
                    values = values,
                    runJsValidator = runValidator,
                )
            } else {
                serviceNameError = res.getString(BaseR.string.service_name_cannot_be_empty)
            }
        },
        themeResId = CommonUiR.style.ComposeDialogTheme_Large,
    ) {
        ServiceFieldList(
            service = service,
            isAdded = isAdded,
            isUpgrade = uiState.updateInfo != null,
            serviceName = serviceName,
            serviceNameError = serviceNameError,
            onServiceNameChange = {
                serviceChanged = true
                serviceName = it
            },
            viewType = viewType,
            onServiceViewTypeChange = {
                serviceChanged = true
                viewType = it
            },
            requiredFields = StableHolder(requiredFields),
            optionalFields = StableHolder(optionalFields),
            invisibleFields = StableHolder(invisibleFields),
            onConfigValueChange = { config, value ->
                serviceChanged = true
                values[config.key] = value
                viewModel.clearValidationResult(config)
            },
            onShowServiceDetailsClick = { scope.launch { serviceDetailsSheet.expand() } },
            runValidator = runValidator,
            onRunValidatorChange = { runValidator = it },
            isValidating = isValidating,
            validations = StableHolder(validations),
        )
    }

    ServiceDetailsSheet(state = serviceDetailsSheet, service = service)
}

@Composable
private fun ServiceFieldList(
    service: UiServiceManifest,
    isAdded: Boolean,
    isUpgrade: Boolean,
    serviceName: String,
    serviceNameError: String?,
    onServiceNameChange: (String) -> Unit,
    viewType: PostsViewType?,
    onServiceViewTypeChange: (PostsViewType) -> Unit,
    requiredFields: StableHolder<List<ServiceConfig>?>,
    optionalFields: StableHolder<List<ServiceConfig>?>,
    invisibleFields: StableHolder<List<ServiceConfig>?>,
    onConfigValueChange: (ServiceConfig, Any?) -> Unit,
    onShowServiceDetailsClick: () -> Unit,
    runValidator: Boolean,
    onRunValidatorChange: (Boolean) -> Unit,
    isValidating: Boolean,
    validations: StableHolder<Map<String, ValidationResult>>,
    modifier: Modifier = Modifier,
) {
    val res = LocalContext.current.resources
    Column(modifier = modifier.fillMaxWidth()) {
        val state = rememberLazyListState()
        LazyColumn(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScrollBar(state),
        ) {
            if (!service.areApiVersionsCompatible) {
                item {
                    WarningMessage(
                        title = { Text(stringResource(BaseR.string.incompatible_service)) },
                        message = {
                            Text(stringResource(BaseR.string.incompatible_service_warning))

                            val currVer = stringResource(
                                BaseR.string._current_api_version,
                                ServiceApiVersion.get(),
                            )
                            Text(currVer)

                            val minVer = stringResource(
                                BaseR.string._min_supported_api_version,
                                service.minApiVersion,
                            )
                            Text(minVer)

                            val max = service.maxApiVersion
                            if (!max.isNullOrEmpty()) {
                                val maxVer = stringResource(
                                    BaseR.string._max_supported_api_version,
                                    max,
                                )
                                Text(maxVer)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (service.source != ServiceManifest.Source.Builtin) {
                item {
                    if (isUpgrade) {
                        val warning = stringResource(BaseR.string.upgrade_external_service_warning)
                        WarningMessage(
                            title = { Text(stringResource(BaseR.string.external_service)) },
                            message = { Html(warning) },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    } else if (!isAdded) {
                        val warning = stringResource(BaseR.string.add_external_service_warning)
                        WarningMessage(
                            title = { Text(stringResource(BaseR.string.external_service)) },
                            message = { Html(warning) },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            item {
                Header(name = stringResource(BaseR.string.information))

                Spacer(modifier = Modifier.height(8.dp))

                ServiceInfo(
                    service = service,
                    onShowServiceDetailsClick = onShowServiceDetailsClick
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                Header(name = stringResource(BaseR.string.configurations))

                Spacer(modifier = Modifier.height(8.dp))

                if (!invisibleFields.value.isNullOrEmpty()) {
                    FieldsErrors(
                        fields = StableHolder(invisibleFields.value!!),
                        validations = validations,
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = serviceName,
                        onValueChange = onServiceNameChange,
                        modifier = Modifier.weight(1f),
                        enabled = !isValidating,
                        label = { Text(serviceNameError ?: stringResource(BaseR.string.name)) },
                        isError = serviceNameError != null,
                        singleLine = true,
                        maxLines = 1,
                    )

                    ServiceViewTypeSelector(
                        enabled = !isValidating,
                        viewType = viewType,
                        onSelected = onServiceViewTypeChange,
                    )
                }
            }

            if (!requiredFields.value.isNullOrEmpty()) {
                fieldsItems(
                    title = res.getString(BaseR.string.required),
                    fields = requiredFields.value!!,
                    onConfigValueChange = onConfigValueChange,
                    isValidating = isValidating,
                    validations = validations.value,
                )
            }

            if (!optionalFields.value.isNullOrEmpty()) {
                fieldsItems(
                    title = res.getString(BaseR.string.optional),
                    fields = optionalFields.value!!,
                    onConfigValueChange = onConfigValueChange,
                    isValidating = isValidating,
                    validations = validations.value,
                )
            }

            if (!requiredFields.value.isNullOrEmpty() || !optionalFields.value.isNullOrEmpty()) {
                item {
                    val forceValidation = !isAdded && service.forceConfigsValidation == true
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                if (!forceValidation) {
                                    onRunValidatorChange(!runValidator)
                                }
                            },
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FlatSwitch(
                            checked = runValidator,
                            enabled = !forceValidation,
                            onCheckedChange = { onRunValidatorChange(!runValidator) },
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(stringResource(BaseR.string.validate_on_add_or_update))
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(name: String) {
    DoubleEndDashedDivider {
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.primary,
        )
    }
}

@Composable
private fun FieldsErrors(
    fields: StableHolder<List<ServiceConfig>>,
    validations: StableHolder<Map<String, ValidationResult>>,
    modifier: Modifier = Modifier,
) {
    var errorMessages by remember { mutableStateOf("") }

    LaunchedEffect(fields, validations) {
        launch(Dispatchers.Default) {
            errorMessages = fields.value
                .mapNotNull {
                    val failReason = validations.value[it.key]
                        ?.failOrNull()
                        ?.reason ?: return@mapNotNull null
                    "${it.name}: $failReason"
                }
                .joinToString(separator = "\n")
        }
    }

    if (errorMessages.isNotEmpty()) {
        ErrorMessage(modifier = modifier) {
            Text(text = errorMessages)
        }
    }
}

private fun LazyListScope.fieldsItems(
    title: String,
    fields: List<ServiceConfig>,
    onConfigValueChange: (ServiceConfig, Any?) -> Unit,
    isValidating: Boolean,
    validations: Map<String, ValidationResult>,
) {
    item {
        Spacer(modifier = Modifier.height(16.dp))

        val primaryColor = MaterialTheme.colors.primary
        Text(
            text = title,
            modifier = Modifier
                .drawBehind {
                    drawRect(
                        color = primaryColor.copy(alpha = 0.6f),
                        size = size.copy(width = 4.dp.toPx()),
                    )
                }
                .padding(start = 8.dp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }

    itemsIndexed(items = fields) { index, config ->
        Spacer(modifier = Modifier.height(8.dp))

        FieldItem(
            enabled = !isValidating,
            field = config,
            error = validations[config.key]?.failOrNull()?.reason,
            onValueChange = { onConfigValueChange(config, it) },
        )

        if (index != fields.size - 1) {
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ServiceViewTypeSelector(
    enabled: Boolean,
    viewType: PostsViewType?,
    onSelected: (PostsViewType) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSelector by remember { mutableStateOf(false) }

    val items = remember {
        listOf(
            ServiceViewTypeEntity(
                viewType = PostsViewType.List,
                icon = CommonUiR.drawable.ic_view_list,
                title = BaseR.string.list,
            ),
            ServiceViewTypeEntity(
                viewType = PostsViewType.Grid,
                icon = CommonUiR.drawable.ic_view_grid,
                title = BaseR.string.grid,
            ),
            ServiceViewTypeEntity(
                viewType = PostsViewType.FullWidth,
                icon = CommonUiR.drawable.ic_view_full_width,
                title = BaseR.string.full_width,
            ),
            ServiceViewTypeEntity(
                viewType = PostsViewType.Card,
                icon = CommonUiR.drawable.ic_view_card,
                title = BaseR.string.card,
            ),
        )
    }

    val selectedItem = remember(viewType) {
        items.find { it.viewType == viewType } ?: items.first()
    }

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else ContentAlpha.disabled)
    ) {
        ServiceViewTypeItem(
            enabled = enabled,
            item = selectedItem,
            onClick = { showSelector = !showSelector },
            isSelected = true,
            showTitle = true,
        )

        if (showSelector) {
            ServiceViewTypeSelectorPopup(
                onDismissRequest = { showSelector = false },
                items = StableHolder(items),
                selectedItem = selectedItem,
                onSelected = onSelected,
            )
        }
    }
}

@Composable
private fun ServiceViewTypeSelectorPopup(
    onDismissRequest: () -> Unit,
    items: StableHolder<List<ServiceViewTypeEntity>>,
    selectedItem: ServiceViewTypeEntity,
    onSelected: (PostsViewType) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismissRequest,
        offset = DpOffset(0.dp, (-56).dp),
        properties = PopupProperties(focusable = true),
    ) {
        Column(modifier = modifier.width(IntrinsicSize.Max)) {
            var preselected by remember(selectedItem) { mutableStateOf(selectedItem) }

            Text(
                text = stringResource(BaseR.string.view_type),
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        top = 8.dp,
                        end = 16.dp,
                    ),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )

            Box(
                modifier = Modifier
                    .height(220.dp)
                    .padding(16.dp),
            ) {
                when (preselected.viewType) {
                    PostsViewType.List -> {
                        ListViewPlaceholder()
                    }

                    PostsViewType.Grid -> {
                        GridViewPlaceholder()
                    }

                    PostsViewType.FullWidth -> {
                        FullWidthViewPlaceholder()
                    }

                    PostsViewType.Card -> {
                        CardViewPlaceholder()
                    }
                }
            }

            Row {
                for (item in items.value) {
                    val isSelected = item == preselected
                    ServiceViewTypeItem(
                        enabled = true,
                        item = item,
                        onClick = { preselected = item },
                        isSelected = isSelected,
                        showTitle = true,
                    )
                }
            }

            Button(
                onClick = {
                    if (selectedItem.viewType != preselected.viewType) {
                        onSelected(preselected.viewType)
                    }
                    onDismissRequest()
                },
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth(),
            ) {
                Text(stringResource(BaseR.string.select))
            }
        }
    }
}

@Composable
private fun ServiceViewTypeItem(
    enabled: Boolean,
    item: ServiceViewTypeEntity,
    onClick: () -> Unit,
    isSelected: Boolean,
    showTitle: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .widthIn(min = 72.dp)
            .clickable(
                interactionSource = remember(item) { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick,
                enabled = enabled,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val tint = if (isSelected) {
            MaterialTheme.colors.primary
        } else {
            LocalContentColor.current
        }
        val title = stringResource(item.title)
        Icon(
            painter = painterResource(item.icon),
            contentDescription = title,
            tint = tint,
        )

        if (showTitle) {
            Text(
                text = title,
                color = tint,
                fontSize = 13.sp,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceInfo(
    service: UiServiceManifest,
    onShowServiceDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row {
            val icon = service.localFirstResourcePath(
                type = ServiceResource.Type.Icon,
                fallback = { service.icon }
            )
            if (!icon.isNullOrEmpty()) {
                var request by remember(icon) { mutableStateOf(ImageRequest.Url(icon)) }
                AsyncImage(
                    request = request,
                    contentDescription = stringResource(BaseR.string.service_icon),
                    modifier = Modifier.size(48.dp),
                    onState = {
                        if (it is ImageState.Success && request.diskCacheEnabled) {
                            // Always load the latest icon
                            ImageLoader.evictFromDiskCache(request)
                            request = ImageRequest.Url.Builder(icon)
                                .memoryCacheEnabled(false)
                                .diskCacheEnabled(false)
                                .build()
                        }
                    },
                    placeholder = ImageRequest.Url(icon),
                )
                Spacer(modifier = Modifier.width(12.dp))
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Badge(
                    label = { Text("Id") },
                    text = { Text(service.id) },
                )

                Badge(
                    label = { Text(stringResource(BaseR.string.developer)) },
                    text = { Text(service.developer) },
                )

                Badge(
                    label = { Text(stringResource(BaseR.string.version)) },
                    text = { Text(service.version) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val context = LocalContext.current
        RichText(
            content = service.description ?: RichContent.Empty,
            onLinkClick = { Intents.openInBrowser(context, it) },
        )

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            TextButton(onClick = onShowServiceDetailsClick) {
                Text(stringResource(BaseR.string.service_details))
            }
        }
    }
}

@Composable
private fun FieldItem(
    enabled: Boolean,
    field: ServiceConfig,
    error: String?,
    onValueChange: (Any?) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (field) {
        is ServiceConfig.Bool -> {
            BoolFieldItem(
                name = field.name,
                description = field.description,
                value = field.value ?: false,
                onValueChange = { onValueChange(it) },
                enabled = enabled,
                modifier = modifier,
            )
        }

        is ServiceConfig.Cookies -> {
            CookiesFieldItem(
                name = field.name,
                description = field.description,
                requestUrl = field.requestUrl,
                targetUrl = field.targetUrl,
                userAgent = field.userAgent,
                onValueChange = { _, cookies -> onValueChange(cookies) },
                enabled = enabled,
                error = error,
                modifier = modifier,
            )
        }

        is ServiceConfig.CookiesUa -> {
            CookiesFieldItem(
                name = field.name,
                description = field.description,
                requestUrl = field.requestUrl,
                targetUrl = field.targetUrl,
                userAgent = field.userAgent,
                onValueChange = { ua, cookies ->
                    val newValue = CookiesUaValue(
                        cookies = cookies,
                        userAgent = ua,
                    )
                    onValueChange(newValue)
                },
                enabled = enabled,
                error = error,
                modifier = modifier,
            )
        }

        is ServiceConfig.Number -> {
            TextFieldItem(
                name = field.name,
                description = field.description,
                value = field.value?.toString(),
                digitsOnly = true,
                onValueChange = { onValueChange(it) },
                enabled = enabled,
                error = error,
                modifier = modifier,
            )
        }

        is ServiceConfig.Option -> {
            OptionFieldItem(
                name = field.name,
                description = field.description,
                options = ImmutableHolder(field.options),
                value = field.value,
                onValueChange = { onValueChange(it) },
                enabled = enabled,
                modifier = modifier,
            )
        }

        is ServiceConfig.Text -> {
            TextFieldItem(
                name = field.name,
                description = field.description,
                value = field.value,
                digitsOnly = false,
                onValueChange = { onValueChange(it) },
                enabled = enabled,
                error = error,
                modifier = modifier,
            )
        }

        is ServiceConfig.Url -> {
            TextFieldItem(
                name = field.name,
                description = field.description,
                value = field.value,
                digitsOnly = false,
                onValueChange = { onValueChange(it) },
                enabled = enabled,
                error = error,
                modifier = modifier,
            )
        }
    }
}

@Immutable
private data class ServiceViewTypeEntity(
    val viewType: PostsViewType,
    @DrawableRes
    val icon: Int,
    @StringRes
    val title: Int,
)