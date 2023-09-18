package any.ui.settings.files

import android.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.compose.StableHolder
import any.base.log.Logger
import any.base.util.FileUtil
import any.data.entity.SpaceInfo
import any.ui.common.dialog.BasicDialog
import any.ui.common.widget.RoundedProgressBar
import any.ui.settings.SettingsItem
import any.ui.settings.SettingsItemIcon
import kotlin.math.max
import any.base.R as BaseR
import any.ui.common.R as CommonUiR

@Composable
internal fun CleanableItems(
    cleanableItems: StableHolder<List<CleanableItem>>,
    modifier: Modifier = Modifier,
) {
    var itemToClean: CleanableItem? by remember { mutableStateOf(null) }

    var itemToAdjustMaxSize: CleanableItem? by remember { mutableStateOf(null) }

    SettingsItem(
        modifier = modifier,
        iconAlignment = Alignment.Top,
        icon = {
            SettingsItemIcon(painterResource(CommonUiR.drawable.ic_baseline_storage_24))
        },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            for ((index, item) in cleanableItems.value.withIndex()) {
                CleanableItem(
                    sizeDescription = item.name,
                    spaceInfo = item.spaceInfo,
                    showAdjustMaxSize = !item.adjustableMaxSizes.isNullOrEmpty(),
                    onCleanClick = { itemToClean = item },
                    onUpdateMaxSizeClick = { itemToAdjustMaxSize = item }
                )

                if (index != cleanableItems.value.lastIndex) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (itemToClean != null) {
        val item = itemToClean!!
        CleanConfirmDialog(
            onDismissRequest = { itemToClean = null },
            item = item,
        )
    }

    if (itemToAdjustMaxSize != null) {
        val item = itemToAdjustMaxSize!!
        AdjustMaxSizeDialog(
            onDismissRequest = { itemToAdjustMaxSize = null },
            currMaxSize = item.spaceInfo.maxSize,
            sizes = item.adjustableMaxSizes ?: emptyList(),
            onSelectSize = item::updateMaxSize,
        )
    }
}

@Composable
private fun CleanableItem(
    sizeDescription: String,
    spaceInfo: SpaceInfo,
    showAdjustMaxSize: Boolean,
    onCleanClick: () -> Unit,
    onUpdateMaxSizeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val startColor = MaterialTheme.colors.primary
    val endColor = remember(startColor) {
        lerp(startColor, Color.White, 0.6f)
    }
    val progressBrush = remember(startColor) {
        Brush.horizontalGradient(listOf(startColor, endColor))
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$sizeDescription: ${spaceInfo.readableOccupiedSize}",
                modifier = Modifier.weight(1f),
            )

            val iconButtonModifier = Modifier
                .size(24.dp)
                .background(
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.1f),
                    shape = CircleShape,
                )
                .padding(4.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showAdjustMaxSize) {
                    Icon(
                        painter = painterResource(CommonUiR.drawable.ic_settings),
                        contentDescription = "Settings",
                        modifier = iconButtonModifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = false),
                                onClick = onUpdateMaxSizeClick,
                            ),
                    )
                }

                Icon(
                    painter = painterResource(CommonUiR.drawable.ic_outline_delete_sweep_24),
                    contentDescription = "Clean",
                    modifier = iconButtonModifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false),
                            onClick = onCleanClick,
                        ),
                )
            }

        }

        Spacer(modifier = Modifier.height(6.dp))

        RoundedProgressBar(
            progress = spaceInfo.occupiedPercent,
            modifier = Modifier.fillMaxWidth(),
            progressBrush = progressBrush,
            startLabel = String.format("%d %%", (spaceInfo.occupiedPercent * 100).toInt()),
            endLabel = spaceInfo.readableMaxSize,
            secondaryProgress = 1f - spaceInfo.availablePercent,
        )
    }
}

@Composable
private fun CleanConfirmDialog(
    onDismissRequest: () -> Unit,
    item: CleanableItem,
    modifier: Modifier = Modifier,
) {
    BasicDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(BaseR.string._clean, item.name)) },
        cancelText = { Text(stringResource(R.string.cancel)) },
        confirmText = {
            Text(
                text = stringResource(BaseR.string.clean),
                color = MaterialTheme.colors.error,
            )
        },
        onConfirmClick = { item.clean() },
        modifier = modifier,
    ) {
        Column {
            Text(stringResource(BaseR.string.clean_item_alert))
            if (item.cleanDescription.isNotEmpty()) {
                Text(
                    text = item.cleanDescription,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun AdjustMaxSizeDialog(
    onDismissRequest: () -> Unit,
    currMaxSize: Long,
    sizes: List<Long>,
    onSelectSize: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currMaxSizeIndex by remember(currMaxSize, sizes) {
        mutableIntStateOf(sizes.indexOf(currMaxSize))
    }

    var sliderValue by remember { mutableFloatStateOf(currMaxSizeIndex.toFloat()) }

    val currSizeText by remember {
        derivedStateOf {
            if (currMaxSizeIndex >= 0) {
                FileUtil.byteCountToString(sizes[currMaxSizeIndex])
            } else {
                FileUtil.byteCountToString(currMaxSize)
            }
        }
    }

    BasicDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(BaseR.string.max_size))

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = currSizeText,
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colors.primary,
                            shape = CircleShape,
                        )
                        .padding(horizontal = 6.dp),
                    color = MaterialTheme.colors.onPrimary,
                    fontSize = 14.sp,
                )
            }
        },
        confirmText = { Text(stringResource(id = R.string.ok)) },
        cancelText = { Text(stringResource(id = R.string.cancel)) },
        onConfirmClick = { onSelectSize(sizes[currMaxSizeIndex]) }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (sizes.isNotEmpty()) {
                    val text = remember(sizes) { FileUtil.byteCountToString(sizes.first()) }
                    Text(text)
                }

                if (sizes.size > 1) {
                    val text = remember(sizes) { FileUtil.byteCountToString(sizes.last()) }
                    Text(text)
                }
            }

            Slider(
                value = sliderValue,
                onValueChange = {
                    Logger.d("Slider", "onValueChange: $it")
                    sliderValue = it
                    currMaxSizeIndex = it.toInt()
                },
                valueRange = 0f..sizes.lastIndex.toFloat(),
                steps = max(0, sizes.size - 2),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

}