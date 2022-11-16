package any.ui.browser

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Checkbox
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.util.performLongPress
import any.ui.common.theme.pass
import any.ui.common.theme.secondaryText
import any.ui.common.theme.warn
import any.ui.common.widget.AnimatedPopup
import any.ui.common.widget.AnimatedPopupItem
import any.ui.common.widget.BasicDialog
import any.ui.common.widget.rememberAnimatedPopupDismissRequester

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TitleBar(
    title: String,
    loadState: LoadState,
    onBackClick: () -> Unit,
    onStopLoadingClick: () -> Unit,
    onReloadClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onShareClick: () -> Unit,
    onOpenInBrowserClick: () -> Unit,
    onRequestClearBrowsingData: (clearCache: Boolean, clearCookies: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    var showClearBrowsingDataDialog by remember { mutableStateOf(false) }

    Column {
        Divider()

        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(BaseR.string.back),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (subtitle != null) {
                    val hapticFeedback = LocalHapticFeedback.current
                    Text(
                        text = subtitle,
                        modifier = Modifier.combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = {
                                onCopyLinkClick()
                                hapticFeedback.performLongPress()
                            },
                        ),
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Box {
                var showMoreMenu by remember { mutableStateOf(false) }

                IconButton(onClick = { showMoreMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(BaseR.string.more_options),
                    )
                }

                if (showMoreMenu) {
                    BrowserMenu(
                        loadState = loadState,
                        onDismissRequest = { showMoreMenu = false },
                        onClearBrowsingDataClick = { showClearBrowsingDataDialog = true },
                        onStopLoadingClick = onStopLoadingClick,
                        onReloadClick = onReloadClick,
                        onCopyLinkClick = onCopyLinkClick,
                        onShareClick = onShareClick,
                        onOpenInBrowserClick = onOpenInBrowserClick,
                        offset = DpOffset(((-16).dp), ((-12).dp)),
                    )
                }
            }
        }
    }

    if (showClearBrowsingDataDialog) {
        ClearBrowsingDataDialog(
            onDismissRequest = { showClearBrowsingDataDialog = false },
            onRequestClearBrowsingData = onRequestClearBrowsingData,
        )
    }
}

@Composable
private fun BrowserMenu(
    loadState: LoadState,
    onDismissRequest: () -> Unit,
    onClearBrowsingDataClick: () -> Unit,
    onStopLoadingClick: () -> Unit,
    onReloadClick: () -> Unit,
    onCopyLinkClick: () -> Unit,
    onShareClick: () -> Unit,
    onOpenInBrowserClick: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
) {
    val popupDismissRequester = rememberAnimatedPopupDismissRequester()

    val isLoading = loadState is LoadState.Loading

    AnimatedPopup(
        dismissRequester = popupDismissRequester,
        onDismissed = onDismissRequest,
        modifier = modifier,
        offset = offset,
        contentAlignmentToAnchor = Alignment.BottomEnd,
        contentPadding = PaddingValues(bottom = 8.dp),
    ) {
        AnimatedPopupItem(
            index = 0,
            onClick = null,
            contentPadding = PaddingValues(0.dp),
        ) {
            Header(
                onClearBrowsingDataClick = {
                    onClearBrowsingDataClick()
                    popupDismissRequester.dismiss()
                },
                state = loadState,
            )
        }

        Divider(modifier = Modifier.alpha(0.5f))

        BrowserMenuItem(
            index = 0,
            onClick = {
                onShareClick()
                popupDismissRequester.dismiss()
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(BaseR.string.share),
                )
            },
            title = { Text(stringResource(BaseR.string.share)) },
        )

        BrowserMenuItem(
            index = 1,
            onClick = {
                onCopyLinkClick()
                popupDismissRequester.dismiss()
            },
            icon = {
                Icon(
                    painter = painterResource(CommonUiR.drawable.ic_baseline_link_24),
                    contentDescription = stringResource(BaseR.string.copy_url),
                )
            },
            title = { Text(stringResource(BaseR.string.copy_url)) },
        )

        BrowserMenuItem(
            index = 2,
            onClick = {
                onOpenInBrowserClick()
                popupDismissRequester.dismiss()
            },
            icon = {
                Icon(
                    painter = painterResource(CommonUiR.drawable.ic_baseline_open_in_browser_24),
                    contentDescription = stringResource(BaseR.string.open_in_external_browser),
                )
            },
            title = { Text(stringResource(BaseR.string.open_in_external_browser)) },
        )

        BrowserMenuItem(
            index = 3,
            onClick = {
                if (isLoading) {
                    onStopLoadingClick()
                } else {
                    onReloadClick()
                }
                popupDismissRequester.dismiss()
            },
            icon = {
                if (isLoading) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(BaseR.string.stop_loading),
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(BaseR.string.reload),
                    )
                }
            },
            title = {
                if (isLoading) {
                    Text(stringResource(BaseR.string.stop_loading))
                } else {
                    Text(stringResource(BaseR.string.reload))
                }
            },
        )
    }
}

@Composable
private fun ClearBrowsingDataDialog(
    onDismissRequest: () -> Unit,
    onRequestClearBrowsingData: (clearCache: Boolean, clearCookies: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var clearCache by remember { mutableStateOf(true) }
    var clearCookies by remember { mutableStateOf(false) }

    val confirmEnabled = clearCache || clearCookies

    BasicDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        title = { Text(stringResource(BaseR.string.clear_browsing_data)) },
        cancelText = { Text(stringResource(android.R.string.cancel)) },
        confirmText = {
            Text(
                text = stringResource(BaseR.string.clear),
                color = MaterialTheme.colors.error.copy(
                    alpha = if (confirmEnabled) 1f else ContentAlpha.disabled
                ),
            )
        },
        onConfirmClick = { onRequestClearBrowsingData(clearCache, clearCookies) },
        confirmEnabled = confirmEnabled,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            BrowsingDataItem(
                isSelected = clearCache,
                onSelectedChange = { clearCache = it },
                icon = {
                    Icon(
                        painter = painterResource(CommonUiR.drawable.ic_baseline_data_usage_24),
                        contentDescription = null,
                    )
                },
            ) {
                Text(stringResource(BaseR.string.cache))
            }

            BrowsingDataItem(
                isSelected = clearCookies,
                onSelectedChange = { clearCookies = it },
                icon = {
                    Icon(
                        painter = painterResource(CommonUiR.drawable.ic_baseline_cookie_24),
                        contentDescription = null,
                    )
                },
            ) {
                Text(stringResource(BaseR.string.cookies))
            }
        }
    }
}

@Composable
private fun BrowsingDataItem(
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = { onSelectedChange(!isSelected) },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                icon()

                Spacer(modifier = Modifier.width(16.dp))
            }

            title()
        }

        Checkbox(checked = isSelected, onCheckedChange = onSelectedChange)
    }
}

@Composable
private fun Header(
    onClearBrowsingDataClick: () -> Unit,
    state: LoadState,
    modifier: Modifier = Modifier,
) {
    val icon: ImageVector
    val iconColor: Color
    val text: String
    when (state) {
        is LoadState.Loading -> {
            icon = Icons.Default.Info
            iconColor = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
            text = stringResource(BaseR.string.page_is_loading)
        }
        is LoadState.Finished -> {
            val sec = String.format("%.1f", state.timeElapse / 1000f)
            val time = " (${sec}s)"
            if (state.isSecure) {
                icon = Icons.Default.CheckCircle
                iconColor = MaterialTheme.colors.pass
                text = stringResource(BaseR.string.secure) + time
            } else {
                icon = Icons.Default.Info
                iconColor = MaterialTheme.colors.warn
                text = stringResource(BaseR.string.insecure) + time
            }
        }
    }
    val backgroundColor = iconColor.copy(alpha = 0.07f)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 2.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = iconColor,
            )

            Spacer(modifier = Modifier.width(18.dp))

            Text(
                text = text,
                fontSize = 13.sp,
            )
        }

        Icon(
            painter = painterResource(CommonUiR.drawable.ic_baseline_delete_outline_24),
            contentDescription = stringResource(BaseR.string.clear_browsing_data),
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colors.onBackground.copy(alpha = 0.08f))
                .clickable(onClick = onClearBrowsingDataClick)
                .padding(2.dp),
            tint = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
        )
    }
}

@Composable
private fun BrowserMenuItem(
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
    title: @Composable () -> Unit,
) {
    AnimatedPopupItem(
        index = index,
        onClick = onClick,
        modifier = modifier,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()

            Spacer(modifier = Modifier.width(16.dp))

            title()
        }
    }
}
