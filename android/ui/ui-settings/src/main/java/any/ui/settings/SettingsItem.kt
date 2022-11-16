package any.ui.settings

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.util.performLongPress
import any.ui.common.theme.secondaryText

private val itemPadding = 12.dp

private val iconSize = 32.dp

@Composable
internal fun SettingsItemIcon(
    painter: Painter,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = MaterialTheme.colors.primary,
) {
    Icon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier
            .size(iconSize)
            .padding(4.dp),
        tint = tint,
    )
}

@Composable
internal fun SettingsItemIcon(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tint: Color = MaterialTheme.colors.primary,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier
            .size(iconSize)
            .padding(4.dp),
        tint = tint,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingsItem(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    iconAlignment: Alignment.Vertical = Alignment.CenterVertically,
    icon: @Composable (() -> Unit)? = null,
    widget: @Composable (() -> Unit)? = null,
    summary: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = onClick != null || onLongClick != null,
                onClick = { onClick?.invoke() },
                onLongClick = {
                    if (onLongClick != null) {
                        hapticFeedback.performLongPress()
                        onLongClick()
                    }
                },
            )
            .padding(itemPadding),
        verticalAlignment = Alignment.Top
    ) {

        if (icon != null) {
            Box(modifier = Modifier.align(iconAlignment)) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colors.primary
                ) {
                    icon()
                }
            }
        } else {
            Spacer(modifier = Modifier.size(iconSize))
        }

        Spacer(modifier = Modifier.width(itemPadding))

        Row {
            Column(
                modifier = Modifier
                    .heightIn(min = iconSize)
                    .weight(weight = 1f, fill = true)
                    .align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.Center,
            ) {
                content()
                if (summary != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    CompositionLocalProvider(
                        LocalTextStyle provides LocalTextStyle.current.copy(
                            color = MaterialTheme.colors.secondaryText,
                            fontSize = 14.sp,
                        )
                    ) {
                        summary()
                    }
                }
            }

            if (widget != null) {
                widget()
            }
        }
    }
}