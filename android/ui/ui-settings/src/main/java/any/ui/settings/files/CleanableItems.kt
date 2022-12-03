package any.ui.settings.files

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import any.base.compose.StableHolder
import any.data.entity.SpaceInfo
import any.ui.common.widget.BasicDialog
import any.ui.common.widget.RoundedProgressBar
import any.ui.settings.SettingsItem
import any.ui.settings.SettingsItemIcon
import any.ui.settings.files.viewmodel.CleanableItem

@Composable
internal fun CleanableItems(
    cleanableItems: StableHolder<List<CleanableItem>>,
    modifier: Modifier = Modifier,
) {
    var itemToClean: CleanableItem? by remember { mutableStateOf(null) }

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
                    onCleanClick = { itemToClean = item },
                )

                if (index != cleanableItems.value.lastIndex) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    if (itemToClean != null) {
        val item = itemToClean!!
        BasicDialog(
            onDismissRequest = { itemToClean = null },
            title = { Text(stringResource(BaseR.string._clean, item.name)) },
            cancelText = { Text(stringResource(android.R.string.cancel)) },
            confirmText = {
                Text(
                    text = stringResource(BaseR.string.clean),
                    color = MaterialTheme.colors.error,
                )
            },
            onConfirmClick = { item.clean() },
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
}

@Composable
private fun CleanableItem(
    sizeDescription: String,
    spaceInfo: SpaceInfo,
    onCleanClick: () -> Unit,
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

            Icon(
                painter = painterResource(CommonUiR.drawable.ic_outline_delete_sweep_24),
                contentDescription = "Clean",
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.1f),
                        shape = CircleShape,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false),
                        onClick = onCleanClick,
                    )
                    .padding(4.dp),
            )
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