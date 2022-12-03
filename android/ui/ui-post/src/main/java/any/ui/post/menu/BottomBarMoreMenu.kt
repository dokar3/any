package any.ui.post.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import any.base.compose.ImmutableHolder
import any.base.util.compose.performTextHandleMove
import any.domain.entity.UiPost
import any.ui.common.widget.AnimatedPopup
import any.ui.common.widget.AnimatedPopupItem
import any.ui.common.widget.rememberAnimatedPopupDismissRequester

@Composable
internal fun BottomBarMoreMenu(
    showDownloadHeader: Boolean,
    post: UiPost?,
    items: ImmutableHolder<List<PostMenuItem>>,
    onDismissRequest: () -> Unit,
    onItemClick: (PostMenuItem) -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
) {
    val dismissRequester = rememberAnimatedPopupDismissRequester()
    val hapticFeedback = LocalHapticFeedback.current
    AnimatedPopup(
        dismissRequester = dismissRequester,
        onDismissed = onDismissRequest,
        modifier = modifier,
        contentAlignmentToAnchor = Alignment.BottomEnd,
        offset = offset,
        properties = PopupProperties(focusable = true),
        contentPadding = PaddingValues(
            top = if (post != null && showDownloadHeader) 0.dp else 8.dp,
            bottom = 8.dp,
        ),
    ) {
        if (post != null && showDownloadHeader) {
            AnimatedPopupItem(
                index = 0,
                onClick = null,
                contentPadding = PaddingValues(0.dp),
            ) {
                Column {
                    DownloadItem(
                        post = post,
                        modifier = Modifier.widthIn(min = 220.dp),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 8.dp,
                            end = 8.dp,
                            bottom = 8.dp,
                        ),
                    )

                    Divider()
                }
            }
        }

        for (i in items.value.indices) {
            val item = items.value[i]
            AnimatedPopupItem(
                index = i + 1,
                onClick = {
                    hapticFeedback.performTextHandleMove()
                    onItemClick(item)
                    dismissRequester.dismiss()
                },
                contentPadding = PaddingValues(
                    start = 0.dp,
                    top = 10.dp,
                    end = 16.dp,
                    bottom = 10.dp
                ),
            ) {
                PostPopupMenuItem(item = item)
            }
        }
    }
}

@Composable
internal fun PostPopupMenuItem(
    item: PostMenuItem,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val primaryColor = MaterialTheme.colors.primary
        Spacer(
            modifier = Modifier
                .width(16.dp)
                .fillMaxHeight()
                .drawBehind {
                    if (item.isSelected) {
                        val center = this.center
                        drawCircle(
                            color = primaryColor,
                            radius = 3.dp.toPx(),
                            center = center.copy(x = center.x + 2.dp.toPx()),
                        )
                    }
                }
        )

        val title = stringResource(item.title)

        Icon(
            painter = painterResource(item.icon),
            contentDescription = title,
            tint = if (item.isSelected) primaryColor else LocalContentColor.current,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Text(text = title)
    }
}
