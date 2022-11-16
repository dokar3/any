package any.ui.home.downloads

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import any.base.ImmutableHolder
import any.base.image.ImageRequest
import any.base.util.performLongPress
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.navPushEvent
import any.navigation.post
import any.navigation.settings
import any.ui.common.CheckableItem
import any.ui.common.image.AsyncImage
import any.ui.common.lazy.rememberLazyListScrollableState
import any.ui.common.modifier.fabOffset
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.theme.imagePlaceholder
import any.ui.common.theme.pass
import any.ui.common.theme.secondaryText
import any.ui.common.theme.sizes
import any.ui.common.theme.thumb
import any.ui.common.theme.thumbBorder
import any.ui.common.widget.BasicDialog
import any.ui.common.widget.EmojiEmptyContent
import any.ui.common.widget.LinearProgressBar
import any.ui.common.widget.QuickReturnScreen
import any.ui.common.widget.RoundedProgressBar
import any.ui.common.widget.TitleActionButton
import any.ui.common.widget.rememberQuickReturnScreenState
import any.ui.home.HomeScreenDefaults
import any.ui.home.HomeScrollToTopManager
import any.ui.home.ScrollToTopHandler
import any.ui.home.SettingsButton
import any.ui.home.TitleBar
import any.ui.home.downloads.viewmodel.DownloadsViewModel
import any.ui.home.downloads.viewmodel.PostDownload
import kotlinx.coroutines.launch

@Composable
internal fun DownloadsScreen(
    onNavigate: (NavEvent) -> Unit,
    onBottomBarOffsetUpdate: (Int) -> Unit,
    titleBarHeight: Dp,
    bottomBarHeight: Dp,
    scrollToTopManager: HomeScrollToTopManager,
    modifier: Modifier = Modifier,
    viewModel: DownloadsViewModel = viewModel(
        factory = DownloadsViewModel.Factory(LocalContext.current)
    ),
) {
    val scope = rememberCoroutineScope()

    val scrollableState = rememberLazyListScrollableState()
    val screenState = rememberQuickReturnScreenState(lazyScrollableState = scrollableState)

    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listPadding = PaddingValues(
        top = topInset + titleBarHeight,
        bottom = bottomBarHeight + bottomInset,
    )

    val uiState by viewModel.downloadsUiState.collectAsState()

    val isInSelection = uiState.isInSelection()

    var bottomBarOffset by remember { mutableStateOf(0) }

    var showRemoveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.loadAllDownloads()
    }

    LaunchedEffect(screenState, isInSelection) {
        if (isInSelection) {
            screenState.resetTopBar()
        }
    }

    DisposableEffect(scrollableState, scrollToTopManager) {
        val handler = ScrollToTopHandler {
            scope.launch {
                scrollableState.quickScrollToTop()
            }
        }
        scrollToTopManager.addHandler(handler)
        onDispose {
            scrollToTopManager.removeHandler(handler)
        }
    }

    BackHandler(enabled = isInSelection) {
        viewModel.unselectAll()
    }

    QuickReturnScreen(
        state = screenState,
        fixedTopBar = isInSelection,
        bottomBarHeight = listPadding.calculateBottomPadding(),
        topBar = {
            TitleBar(
                height = titleBarHeight,
                startActionButton = {
                    if (isInSelection) {
                        TitleActionButton(
                            label = stringResource(BaseR.string.unselect_all),
                            onClick = { viewModel.unselectAll() },
                        ) { label ->
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = label,
                            )
                        }
                    } else {
                        SettingsButton(onClick = {
                            val route = Routes.settings(subSettings = Routes.Settings.FILES)
                            onNavigate(navPushEvent(route))
                        })
                    }
                },
                endActionButton = {
                    if (isInSelection) {
                        TitleActionButton(
                            label = stringResource(BaseR.string.select_all),
                            onClick = { viewModel.selectAll() },
                        ) { label ->
                            Icon(
                                painter = painterResource(
                                    CommonUiR.drawable.ic_baseline_done_all_24
                                ),
                                contentDescription = label,
                            )
                        }
                    }
                },
            ) {
                if (isInSelection) {
                    val selectedCount = uiState.selectedDownloadUrls.size
                    Text(stringResource(BaseR.string._selected, selectedCount))
                } else {
                    Row {
                        Text(stringResource(BaseR.string.downloads))
                        Text(
                            text = " (${uiState.downloads.size})",
                            color = MaterialTheme.colors.secondaryText,
                        )
                    }
                }
            }
        },
        bottomBar = {
            onBottomBarOffsetUpdate(it)
            bottomBarOffset = it
        },
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (!uiState.isLoadingDownloads && uiState.downloads.isEmpty()) {
                EmojiEmptyContent {
                    Text(stringResource(BaseR.string.no_downloads))
                }
            } else {
                DownloadList(
                    onNavigate = onNavigate,
                    onSelect = viewModel::select,
                    onUnselect = viewModel::unselect,
                    state = scrollableState.listState,
                    isInSelection = uiState.isInSelection(),
                    downloads = ImmutableHolder(uiState.downloads),
                    selectedDownloads = ImmutableHolder(uiState.selectedDownloadUrls),
                    contentPadding = listPadding,
                )
            }

            if (uiState.isLoadingDownloads) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(stringResource(BaseR.string.loading))
                }
            }

            if (isInSelection) {
                val navBarHeightPx = WindowInsets.navigationBars
                    .getBottom(LocalDensity.current)
                FloatingActionButton(
                    onClick = { showRemoveDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .fabOffset()
                        .offset {
                            val btmBarHeightPx = bottomBarHeight.roundToPx()
                            val btmOffset = bottomBarOffset.coerceAtMost(
                                btmBarHeightPx - navBarHeightPx
                            )
                            val fabOffset = -(btmBarHeightPx - btmOffset)
                            IntOffset(x = 0, y = fabOffset)
                        },
                    backgroundColor = MaterialTheme.colors.error,
                    contentColor = MaterialTheme.colors.onError,
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(BaseR.string.remove_selected),
                    )
                }
            }
        }
    }

    if (showRemoveDialog) {
        var deleteFiles by remember { mutableStateOf(false) }
        BasicDialog(
            onDismissRequest = { showRemoveDialog = false },
            title = { Text(stringResource(BaseR.string.remove_downloads)) },
            cancelText = { Text(stringResource(android.R.string.cancel)) },
            confirmText = {
                Text(
                    text = stringResource(BaseR.string.remove),
                    color = MaterialTheme.colors.error,
                )
            },
            onConfirmClick = {
                viewModel.removeSelectedDownloads(deleteFiles)
            }
        ) {
            Column {
                Text(stringResource(BaseR.string.remove_selected_downloads_alert))

                val interactionSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { deleteFiles = !deleteFiles }
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = deleteFiles,
                        onCheckedChange = { deleteFiles = it },
                        interactionSource = interactionSource,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(stringResource(BaseR.string.delete_downloaded_files))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadList(
    onNavigate: (NavEvent) -> Unit,
    onSelect: (PostDownload) -> Unit,
    onUnselect: (PostDownload) -> Unit,
    state: LazyListState,
    isInSelection: Boolean,
    downloads: ImmutableHolder<List<PostDownload>>,
    selectedDownloads: ImmutableHolder<Set<String>>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .verticalScrollBar(
                state = state,
                padding = contentPadding,
            ),
        state = state,
        contentPadding = contentPadding,
    ) {
        items(
            items = downloads.value,
            key = { it.serviceId + it.url },
        ) {
            val isSelected = selectedDownloads.value.contains(it.url)
            DownloadItem(
                download = it,
                isSelected = isSelected,
                isInSelection = isInSelection,
                onClick = {
                    if (isInSelection) {
                        if (isSelected) {
                            onUnselect(it)
                        } else {
                            onSelect(it)
                        }
                    } else {
                        onNavigate(
                            navPushEvent(
                                Routes.post(url = it.url, serviceId = it.serviceId)
                            )
                        )
                    }
                },
                onLongClick = {
                    if (isInSelection) {
                        if (isSelected) {
                            onUnselect(it)
                        } else {
                            onSelect(it)
                        }
                    } else {
                        onSelect(it)
                    }
                },
                onActionButtonClick = {
                    if (it.isDownloading || it.isWaiting) {
                        it.onCancel()
                    } else if (!it.isComplete) {
                        it.onStart()
                    }
                },
                modifier = Modifier.animateItemPlacement(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DownloadItem(
    download: PostDownload,
    isSelected: Boolean,
    isInSelection: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onActionButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hapticFeedback = LocalHapticFeedback.current
    CheckableItem(
        isChecked = isSelected,
        showCheckmark = isInSelection,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(),
                onClick = onClick,
                onLongClick = {
                    hapticFeedback.performLongPress()
                    onLongClick()
                },
            )
            .padding(HomeScreenDefaults.ListItemPadding)
    ) {
        DownloadItemContent(
            isInSelection = isInSelection,
            download = download,
            onActionButtonClick = onActionButtonClick,
        )
    }
}

@Composable
private fun DownloadItemContent(
    isInSelection: Boolean,
    download: PostDownload,
    onActionButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
    thumbnailWidth: Dp = 60.dp,
    defThumbAspectRatio: Float = 4f / 5,
) {
    Row(modifier = modifier) {
        val thumbnail = download.thumbnail
        if (!thumbnail.isNullOrEmpty()) {
            AsyncImage(
                request = ImageRequest.Url(download.thumbnail),
                contentDescription = stringResource(BaseR.string.cover),
                modifier = Modifier
                    .width(thumbnailWidth)
                    .aspectRatio(download.thumbnailAspectRatio ?: defThumbAspectRatio)
                    .clip(MaterialTheme.shapes.thumb)
                    .border(
                        width = MaterialTheme.sizes.thumbBorderStroke,
                        color = MaterialTheme.colors.thumbBorder,
                        shape = MaterialTheme.shapes.thumb,
                    )
                    .background(MaterialTheme.colors.imagePlaceholder),
                contentScale = ContentScale.Crop,
            )

            Spacer(modifier = Modifier.width(16.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = thumbnailWidth * defThumbAspectRatio),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = download.title,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (download.isComplete) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = "Downloaded check mark",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colors.pass,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        if (download.isPreparing) {
                            Text(stringResource(BaseR.string.preparing))
                        } else {
                            Text("${download.downloaded} / ${download.total}")
                        }

                        if (download.downloadedSize != null) {
                            Spacer(modifier = Modifier.width(8.dp))

                            Spacer(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(
                                        color = MaterialTheme.colors.secondaryText,
                                        shape = CircleShape,
                                    )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(download.downloadedSize)
                        }
                    }
                }

                if (!download.isComplete && !isInSelection) {
                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = onActionButtonClick,
                        modifier = Modifier
                            .width(56.dp)
                            .height(32.dp),
                        shape = CircleShape,
                    ) {
                        if (download.isDownloading || download.isWaiting) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = stringResource(android.R.string.cancel),
                                tint = MaterialTheme.colors.error,
                            )
                        } else {
                            Icon(
                                painter = painterResource(CommonUiR.drawable.ic_download),
                                contentDescription = stringResource(BaseR.string.downloads),
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (download.isPreparing) {
                LinearProgressBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape),
                    height = 8.dp,
                )
            } else if (!download.isComplete) {
                val downloaded = download.downloaded
                val total = download.total
                val progress = if (total <= 0) 0f else downloaded.toFloat() / total
                val progressColor = if (download.isDownloading) {
                    MaterialTheme.colors.primary
                } else if (download.isFailure) {
                    MaterialTheme.colors.error
                } else {
                    MaterialTheme.colors.onBackground.copy(alpha = 0.3f)
                }
                RoundedProgressBar(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth(),
                    progressColor = progressColor,
                    animateProgress = download.isDownloading,
                )
            }
        }
    }
}
