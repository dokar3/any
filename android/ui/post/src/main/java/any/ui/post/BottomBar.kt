package any.ui.post

import any.base.R as BaseR
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import any.base.compose.ImmutableHolder
import any.domain.entity.UiPost
import any.ui.common.theme.bottomBarBackground
import any.ui.common.widget.NavigationBarSpacer
import any.ui.common.widget.ShadowDividerSpacer
import any.ui.common.widget.TooltipBox
import any.ui.common.widget.TooltipPosition
import any.ui.post.menu.BottomBarMoreMenu
import any.ui.post.menu.PostMenuItem

@Composable
internal fun BottomBar(
    showDownload: Boolean,
    post: UiPost?,
    items: ImmutableHolder<List<PostMenuItem>>,
    moreItems: ImmutableHolder<List<PostMenuItem>>,
    onItemClick: (PostMenuItem) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    moreItemsEnabled: Boolean = true,
    menuIconDecoration: @Composable (BoxScope.(PostMenuItem) -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colors.bottomBarBackground,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ShadowDividerSpacer()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .background(backgroundColor),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                for (item in items.value) {
                    TooltipBox(
                        text = stringResource(item.title),
                        modifier = Modifier.weight(1f),
                        position = TooltipPosition.AboveContent,
                        offset = DpOffset(0.dp, (-8).dp),
                    ) {
                        MenuItem(
                            item = item,
                            showTitle = items.value.size <= 2,
                            onClick = {
                                onItemClick(item)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            iconDecoration = {
                                if (menuIconDecoration != null) {
                                    menuIconDecoration(item)
                                }
                            }
                        )
                    }
                }
            }

            if (moreItemsEnabled && moreItems.value.isNotEmpty()) {
                var showMoreItems by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .clickable { showMoreItems = true },
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(BaseR.string.more_options),
                        modifier = Modifier.align(Alignment.Center),
                        tint = MaterialTheme.colors.primary,
                    )

                    if (showMoreItems) {
                        BottomBarMoreMenu(
                            showDownloadHeader = showDownload,
                            post = post,
                            items = moreItems,
                            onDismissRequest = { showMoreItems = false },
                            onItemClick = onItemClick,
                            offset = DpOffset((-16).dp, (-16).dp)
                        )
                    }
                }
            }
        }

        NavigationBarSpacer(modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun MenuItem(
    item: PostMenuItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconDecoration: @Composable (BoxScope.() -> Unit)? = null,
    showTitle: Boolean = true,
) {
    Row(
        modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val title = stringResource(item.title)
        Box {
            Icon(
                painter = painterResource(item.icon),
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colors.primary,
            )
            if (iconDecoration != null) {
                iconDecoration()
            }
        }
        if (showTitle && title.isNotEmpty()) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(title)
        }
    }
}
