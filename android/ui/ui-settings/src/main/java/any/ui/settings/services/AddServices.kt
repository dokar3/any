package any.ui.settings.services

import any.base.R as BaseR
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import any.base.compose.ImmutableHolder
import any.data.entity.ServiceManifest
import any.domain.entity.UiServiceManifest
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.widget.MSG_POPUP_DURATION
import any.ui.common.widget.MessagePopup
import any.ui.common.widget.ProgressBar
import any.ui.common.widget.SearchBar
import any.ui.service.ServiceDialog
import any.ui.settings.services.viewmodel.AddServiceUiState
import any.ui.settings.services.viewmodel.AppendableService
import any.ui.settings.services.viewmodel.ServiceMgtViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun AddServices(
    viewModel: ServiceMgtViewModel,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    val uiState by viewModel.addServiceUiState.collectAsState()

    var message by remember { mutableStateOf("") }
    var showMessage by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        delay(300)
        viewModel.loadAppendableServicesIfEmpty()
    }

    val res = LocalContext.current.resources
    AddServicesContent(
        onServiceAdded = {
            message = res.getString(BaseR.string._service_added_with_name, it.name)
            scope.launch {
                delay(MSG_POPUP_DURATION)
                showMessage = false
            }
        },
        onServiceUpdated = {
            message = res.getString(BaseR.string._service_updated_with_name, it.name)
            scope.launch {
                delay(MSG_POPUP_DURATION)
                showMessage = false
            }
        },
        onUpdateSearchQuery = viewModel::updateSearchQuery,
        uiState = uiState,
        modifier = modifier,
    )

    MessagePopup(
        visible = showMessage,
        onDismissed = { showMessage = false },
        offset = DpOffset(0.dp, (-32).dp),
    ) {
        Text(message)
    }
}

@Composable
private fun AddServicesContent(
    onUpdateSearchQuery: (TextFieldValue) -> Unit,
    onServiceAdded: (ServiceManifest) -> Unit,
    onServiceUpdated: (ServiceManifest) -> Unit,
    uiState: AddServiceUiState,
    modifier: Modifier = Modifier,
) {
    var serviceToConfigure: AppendableService? by remember { mutableStateOf(null) }

    Column(modifier = modifier) {
        SearchBar(
            text = uiState.searchQuery,
            onValueChange = onUpdateSearchQuery,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = {
                Text(
                    text = stringResource(
                        BaseR.string._search_services_with_count,
                        uiState.appendableServices.size
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            AppendableServiceList(
                services = ImmutableHolder(uiState.appendableServices),
                onAddServiceClick = { serviceToConfigure = it },
            )

            if (uiState.isLoadingAppendableServices) {
                Column {
                    ProgressBar(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        size = 36.dp,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(BaseR.string.loading_services),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }

    if (serviceToConfigure != null) {
        val appendableService = serviceToConfigure!!
        ServiceDialog(
            onDismissRequest = { serviceToConfigure = null },
            service = appendableService.service,
            isAdded = appendableService.isAdded,
            onSaveService = {
                if (appendableService.isAdded) {
                    onServiceUpdated(it)
                } else {
                    onServiceAdded(it)
                }
                appendableService.onSaveService(it)
            },
        )
    }
}

@Composable
private fun AppendableServiceList(
    services: ImmutableHolder<List<AppendableService>>,
    onAddServiceClick: (AppendableService) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .verticalScrollBar(listState),
        state = listState,
    ) {
        items(
            items = services.value,
            key = { it.service.id },
            contentType = { "service_item" },
        ) {
            AppendableServiceItem(
                service = it.service,
                isAdded = it.isAdded,
                onAddClick = { onAddServiceClick(it) },
            )
        }
    }
}

@Composable
private fun AppendableServiceItem(
    service: UiServiceManifest,
    isAdded: Boolean,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ServiceItem(
        service = service,
        modifier = modifier
            .clickable { onAddClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        showServiceSource = true,
        actionButton = {
            IconButton(
                onClick = {
                    if (service.areApiVersionsCompatible) {
                        onAddClick()
                    }
                },
            ) {
                val icon = if (isAdded) Icons.Default.Check else Icons.Default.Add
                val tint = if (isAdded) {
                    MaterialTheme.colors.primary
                } else {
                    MaterialTheme.colors.onBackground
                }
                Icon(
                    imageVector = icon,
                    contentDescription = stringResource(BaseR.string.add_service),
                    tint = tint,
                )
            }
        },
    )
}
