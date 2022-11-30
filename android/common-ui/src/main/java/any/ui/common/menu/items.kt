package any.ui.common.menu

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ItemVerticalPadding = 16.dp
private val ItemHorizontalPadding = 12.dp

private val CategoryVerticalPadding = 6.dp
private val CategoryHorizontalPadding = 16.dp

private const val ContentAlphaDisabled = 0.45f

@Composable
fun SwitchMenuItem(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    clickable: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(
        horizontal = ItemHorizontalPadding,
        vertical = ItemVerticalPadding
    ),
    spacing: Dp = 18.dp,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
) {
    MenuItem(
        modifier = modifier,
        enabled = enabled,
        clickable = clickable,
        onClick = {
            onCheckedChange?.invoke(!checked)
            onClick?.invoke()
        },
        onLongClick = onLongClick,
        padding = padding,
        spacing = spacing,
        icon = icon,
        widget = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.heightIn(max = 24.dp),
                enabled = enabled,
            )
        },
        title = title,
    )
}

@Composable
fun CheckBoxMenuItem(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    clickable: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(
        horizontal = ItemHorizontalPadding,
        vertical = ItemVerticalPadding
    ),
    spacing: Dp = 18.dp,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
) {
    MenuItem(
        modifier = modifier,
        enabled = enabled,
        clickable = clickable,
        onClick = {
            onCheckedChange?.invoke(!checked)
            onClick?.invoke()
        },
        onLongClick = onLongClick,
        padding = padding,
        spacing = spacing,
        icon = icon,
        widget = {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.heightIn(max = 24.dp),
                enabled = enabled,
            )
        },
        title = title,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryMenuItem(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    clickable: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
    titlePadding: PaddingValues = PaddingValues(
        horizontal = CategoryHorizontalPadding,
        vertical = CategoryVerticalPadding,
    ),
    title: @Composable () -> Unit,
) {
    val contentAlpha = if (enabled) 1f else ContentAlphaDisabled
    CompositionLocalProvider(
        LocalContentAlpha provides contentAlpha
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(
                    enabled = enabled && clickable,
                    onClick = { onClick?.invoke() },
                    onLongClick = { onLongClick?.invoke() },
                ),
        ) {
            val color = MaterialTheme.colors.primary
            val textStyle = LocalTextStyle.current
            val currTextStyle = remember(textStyle, color) {
                textStyle.copy(
                    color = color,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column {
                CompositionLocalProvider(LocalTextStyle provides currTextStyle) {
                    Box(modifier = Modifier.padding(titlePadding)) {
                        title()
                    }
                }
                if (showDivider) {
                    Divider(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuItem(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    clickable: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(
        horizontal = ItemHorizontalPadding,
        vertical = ItemVerticalPadding
    ),
    spacing: Dp = 18.dp,
    icon: @Composable (() -> Unit)? = null,
    widget: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
) {
    val contentAlpha = if (enabled) 1f else ContentAlphaDisabled
    CompositionLocalProvider(
        LocalContentAlpha provides contentAlpha
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(
                    enabled = enabled && clickable,
                    onClick = { onClick?.invoke() },
                    onLongClick = { onLongClick?.invoke() },
                )
                .padding(padding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                icon()
                Spacer(modifier = Modifier.width(spacing))
            }
            Box(
                modifier = Modifier.weight(1f)
            ) {
                title()
            }
            if (widget != null) {
                Spacer(modifier = Modifier.width(spacing))
                widget()
            }
        }
    }
}
