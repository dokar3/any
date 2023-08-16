package any.ui.common.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.R
import any.base.compose.ImmutableHolder
import any.domain.entity.UpdatableService
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.theme.divider
import any.ui.common.theme.pass
import any.ui.common.theme.secondaryText
import any.ui.common.widget.Avatar
import any.ui.common.widget.SimpleDialog
import java.text.SimpleDateFormat

@Composable
fun UpdateBuiltinServicesDialog(
    onDismissRequest: () -> Unit,
    onUpdateClick: (List<UpdatableService>) -> Unit,
    updatableServices: ImmutableHolder<List<UpdatableService>>,
    modifier: Modifier = Modifier,
    onIgnoreClick: (() -> Unit)? = null,
    showIgnore: Boolean = onIgnoreClick != null,
    dismissOnClickOutside: Boolean = true,
) {
    val serviceList = updatableServices.value

    var updatedCount = 0
    var updatingCount = 0
    var updatableCount = 0
    for (item in serviceList) {
        if (item.isUpdated) {
            updatedCount++
        } else if (!item.isUpdating) {
            updatableCount++
        } else {
            updatingCount++
        }
    }
    val isUpdating = updatingCount > 0
    val isAllUpdated = updatedCount == serviceList.size

    SimpleDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.update)) },
        neutralText = {
            if (showIgnore && !isAllUpdated && !isUpdating) {
                Text(stringResource(R.string.ignore))
            }
        },
        cancelText = { Text(stringResource(android.R.string.cancel)) },
        confirmText = {
            if (isAllUpdated) {
                Text(stringResource(android.R.string.ok))
            } else {
                Text(stringResource(R.string.update_all))
            }
        },
        onNeutralClick = { onIgnoreClick?.invoke() },
        onConfirmClick = { onUpdateClick(serviceList) },
        cancelEnabled = !isUpdating,
        confirmEnabled = updatableCount > 0 || isAllUpdated,
        dismissOnConfirm = isAllUpdated,
        dismissOnClickOutside = if (dismissOnClickOutside) {
            !isUpdating
        } else {
            isAllUpdated
        },
        dismissOnBackPress = !isUpdating,
        modifier = modifier
    ) {
        Column {
            if (updatableCount > 0) {
                Text(stringResource(R.string._builtin_services_update_alert, updatableCount))
            } else {
                Text(stringResource(R.string.all_builtin_services_are_up_to_date))
            }

            Spacer(modifier = Modifier.height(8.dp))

            UpdatableList(
                onUpdate = { onUpdateClick(listOf(it)) },
                infoList = updatableServices,
                modifier = Modifier.heightIn(max = 300.dp),
            )
        }
    }
}

@Composable
private fun UpdatableList(
    onUpdate: (UpdatableService) -> Unit,
    infoList: ImmutableHolder<List<UpdatableService>>,
    modifier: Modifier = Modifier,
) {
    val expandedItems = remember { mutableStateMapOf<Int, Boolean>() }

    val state = rememberLazyListState()

    LazyColumn(
        state = state,
        modifier = modifier.verticalScrollBar(state),
    ) {
        itemsIndexed(infoList.value) { index, item ->
            UpdatableItem(
                service = item,
                isExpanded = expandedItems[index] ?: false,
                onExpand = { expandedItems[index] = true },
                onCollapse = { expandedItems[index] = false },
                onUpdate = { onUpdate(item) },
            )
        }
    }
}

@Composable
private fun UpdatableItem(
    service: UpdatableService,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onCollapse: () -> Unit,
    onUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dividerColor = MaterialTheme.colors.divider
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable {
                if (isExpanded) {
                    onCollapse()
                } else {
                    onExpand()
                }
            }
            .padding(vertical = 8.dp)
            .drawBehind {
                if (isExpanded) {
                    val dividerHeight = 1.dp.toPx()
                    drawLine(
                        color = dividerColor,
                        start = Offset(0f, size.height - dividerHeight),
                        end = Offset(size.width, size.height - dividerHeight),
                        strokeWidth = dividerHeight,
                    )
                }
            },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                name = service.value.name,
                url = service.value.icon,
                size = 36.dp,
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        text = service.value.name,
                        modifier = Modifier.weight(1f),
                    )
                }

                Text(
                    text = "${service.upgradeInfo.fromVersion} → ${service.upgradeInfo.toVersion}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.secondaryText,
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            val updateEnabled = !service.isUpdating && !service.isUpdated
            TextButton(
                onClick = {
                    if (updateEnabled) {
                        onUpdate()
                    }
                },
                modifier = Modifier.height(36.dp),
                enabled = updateEnabled,
                shape = CircleShape,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (updateEnabled) {
                        MaterialTheme.colors.primary
                    } else {
                        Color.Transparent
                    }
                ),
            ) {
                if (service.isUpdated) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colors.pass,
                    )
                } else if (service.isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.update),
                        fontSize = 14.sp,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.build_time),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )

                val date = service.value.buildTime
                val dateString = if (date >= 0) {
                    remember(date) { SimpleDateFormat.getDateTimeInstance().format(date) }
                } else {
                    stringResource(R.string.unknown)
                }
                Text(
                    text = dateString,
                    fontSize = 14.sp,
                )
            }
        }
    }
}