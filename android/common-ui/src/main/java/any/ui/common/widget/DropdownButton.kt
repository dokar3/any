package any.ui.common.widget

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import any.base.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DropdownButton(
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    arrow: @Composable () -> Unit = { AnimatedDropdownArrow(isExpanded = isExpanded) },
    text: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .padding(start = 16.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = { onLongClick?.invoke() },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.weight(weight = 1f, fill = false)) {
            text()
        }

        Spacer(modifier = Modifier.width(4.dp))

        arrow()
    }
}

@Composable
fun AnimatedDropdownArrow(
    isExpanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val iconRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f
    )
    Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = stringResource(R.string.switch_service),
        modifier = modifier.rotate(iconRotation),
    )
}