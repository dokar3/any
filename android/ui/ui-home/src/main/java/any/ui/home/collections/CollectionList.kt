package any.ui.home.collections

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import any.base.R
import any.base.StableHolder
import any.base.prefs.FolderViewType
import any.base.util.joinToPath
import any.data.entity.Folder
import any.domain.entity.UiPost
import any.ui.common.modifier.drawCheckMark
import any.ui.common.modifier.gridItemPadding
import any.ui.common.modifier.rememberLazyGridColumnCount
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.post.CardPostItem
import any.ui.common.post.FullWidthPostItem
import any.ui.common.post.GridPostItem
import any.ui.common.post.ListPostItem
import any.ui.common.theme.sizes
import any.ui.common.widget.EmojiEmptyContent
import any.ui.home.HomeScreenDefaults
import any.ui.home.Loading
import any.ui.home.collections.item.FolderItem
import kotlinx.coroutines.delay

@ExperimentalFoundationApi
@Composable
internal fun CollectionList(
    state: LazyGridState,
    folder: Folder,
    viewType: FolderViewType,
    folders: StableHolder<List<Folder>>,
    posts: StableHolder<List<UiPost>>,
    selectedPosts: StableHolder<Set<UiPost>>,
    isLoading: Boolean,
    onMediaClick: (post: UiPost, index: Int) -> Unit,
    onUserClick: (serviceId: String, userId: String) -> Unit,
    onCommentsClick: (UiPost) -> Unit,
    onLinkClick: (String) -> Unit,
    onPostClick: (UiPost) -> Unit,
    onPostLongClick: (UiPost) -> Unit,
    onFolderClick: (Folder) -> Unit,
    onFolderLongClick: (Folder) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val gridCells = GridCells.Adaptive(minSize = MaterialTheme.sizes.minAdaptiveGridCellsWidth)
    val gridCellSpacing = HomeScreenDefaults.GridItemSpacing
    val columnCount = rememberLazyGridColumnCount(
        state = state,
        cells = gridCells,
        spacing = gridCellSpacing,
    )

    LazyVerticalGrid(
        columns = gridCells,
        modifier = modifier
            .semantics { contentDescription = "CollectionList" }
            .verticalScrollBar(
                state = state,
                padding = contentPadding,
            ),
        state = state,
        contentPadding = contentPadding,
    ) {
        val showFolderBar = !folder.isRoot()

        if (showFolderBar) {
            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "folder_nav_bar",
                contentType = "folder_nav_bar",
            ) {
                FolderNavigationBar(
                    currentFolder = folder,
                    onFolderClick = { onFolderClick(it) },
                )
            }
        }

        itemsIndexed(
            items = folders.value,
            key = { _, folder -> folder.path },
            contentType = { _, _ -> "folder" },
        ) { index, folder ->
            FolderItem(
                folder = folder,
                selectedPosts = selectedPosts,
                onClick = { onFolderClick(folder) },
                onLongClick = { onFolderLongClick(folder) },
                modifier = Modifier
                    .gridItemPadding(
                        spacing = gridCellSpacing,
                        firstRowTopSpacing = if (showFolderBar) {
                            gridCellSpacing
                        } else {
                            gridCellSpacing / 2
                        },
                        columnCount = columnCount,
                        index = index,
                    ),
            )
        }

        when (viewType) {
            FolderViewType.Grid -> {
                itemsIndexed(
                    items = posts.value,
                    key = { _, post -> "${post.serviceId}:${post.url}" },
                    contentType = { _, _ -> "post" },
                ) { index, post ->
                    val isSelected = selectedPosts.value.contains(post)
                    GridPostItem(
                        post = post,
                        defThumbAspectRatio = null,
                        onCollectClick = null,
                        onMoreClick = onPostLongClick,
                        onUserClick = { onUserClick(post.serviceId, it) },
                        onLinkClick = onLinkClick,
                        onClick = onPostClick,
                        onLongClick = onPostLongClick,
                        modifier = Modifier
                            .gridItemPadding(
                                spacing = gridCellSpacing,
                                columnCount = columnCount,
                                index = folders.value.size + index,
                            )
                            .drawCheckMark(visible = isSelected),
                        showCollectButton = false,
                    )
                }
            }

            FolderViewType.List -> {
                items(
                    items = posts.value,
                    key = { post -> "${post.serviceId}:${post.url}" },
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = { "post" },
                ) { post ->
                    val isSelected = selectedPosts.value.contains(post)
                    ListPostItem(
                        post = post,
                        defThumbAspectRatio = null,
                        onCollectClick = null,
                        onCommentsClick = onCommentsClick,
                        onMoreClick = onPostLongClick,
                        onUserClick = { onUserClick(post.serviceId, it) },
                        onLinkClick = onLinkClick,
                        onClick = onPostClick,
                        onLongClick = onPostLongClick,
                        modifier = Modifier.drawCheckMark(visible = isSelected),
                        showCollectButton = false,
                    )
                }
            }

            FolderViewType.FullWidth -> {
                items(
                    items = posts.value,
                    key = { post -> "${post.serviceId}:${post.url}" },
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = { "post" },
                ) { post ->
                    val isSelected = selectedPosts.value.contains(post)
                    FullWidthPostItem(
                        post = post,
                        defThumbAspectRatio = null,
                        showCollectButton = false,
                        onCollectClick = null,
                        onCommentsClick = onCommentsClick,
                        onMoreClick = onPostLongClick,
                        onMediaClick = { p, media -> onMediaClick(p, media) },
                        onUserClick = { onUserClick(post.serviceId, it) },
                        onLinkClick = onLinkClick,
                        onClick = onPostClick,
                        onLongClick = onPostLongClick,
                        modifier = Modifier.drawCheckMark(visible = isSelected),
                    )
                }
            }

            FolderViewType.Card -> {
                items(
                    items = posts.value,
                    key = { post -> "${post.serviceId}:${post.url}" },
                    span = { GridItemSpan(maxLineSpan) },
                    contentType = { "post" },
                ) { post ->
                    val isSelected = selectedPosts.value.contains(post)
                    CardPostItem(
                        post = post,
                        defThumbAspectRatio = null,
                        onCollectClick = null,
                        onCommentsClick = onCommentsClick,
                        onMoreClick = onPostLongClick,
                        onMediaClick = onMediaClick,
                        onUserClick = { onUserClick(post.serviceId, it) },
                        onLinkClick = onLinkClick,
                        onClick = onPostClick,
                        onLongClick = onPostLongClick,
                        modifier = Modifier.drawCheckMark(visible = isSelected),
                        showCollectButton = false,
                    )
                }
            }
        }

        // Add an empty full-span item for the scrollable pixels calculation, because
        // the result may be incorrect if items of the last row have different heights.
        // This item does not require a non-zero height since we have already set the
        // bottom content padding for the grid.
        item(
            span = { GridItemSpan(maxLineSpan) },
            content = {},
        )
    }

    if (folders.value.isEmpty() && posts.value.isEmpty() && !isLoading) {
        EmojiEmptyContent(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(stringResource(R.string.no_posts))
        }
    }

    var showLoading by remember { mutableStateOf(false) }
    LaunchedEffect(isLoading) {
        showLoading = if (isLoading) {
            // If the loading is too fast, do not show the loading ui
            delay(500)
            true
        } else {
            if (showLoading) {
                // Don't let it disappear so quick, show the loading view at least 200ms
                delay(200)
            }
            false
        }
    }
    if (showLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background.copy(alpha = 0.9f)),
            contentAlignment = Alignment.Center,
        ) {
            Loading(stringResource(R.string.loading))
        }
    }
}

@Composable
private fun FolderNavigationBar(
    currentFolder: Folder,
    onFolderClick: (Folder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val segments = remember(currentFolder) {
        if (currentFolder.pathSegments.firstOrNull() != Folder.ROOT.path) {
            listOf(Folder.ROOT.path) + currentFolder.pathSegments
        } else {
            listOf(Folder.ROOT.path)
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(currentFolder) {
        listState.scrollToItem(segments.lastIndex)
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = HomeScreenDefaults.HorizontalPadding),
    ) {
        itemsIndexed(items = segments) { index, item ->
            val isCurrent = index == segments.lastIndex
            Row(
                modifier = Modifier
                    .heightIn(min = 32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCurrent) {
                            MaterialTheme.colors.primary
                        } else {
                            MaterialTheme.colors.background
                        }
                    )
                    .border(
                        border = ButtonDefaults.outlinedBorder,
                        shape = CircleShape
                    )
                    .clickable {
                        val folder = Folder(
                            path = segments
                                .subList(0, index + 1)
                                .joinToPath()
                        )
                        onFolderClick(folder)
                    }
                    .padding(PaddingValues(horizontal = 12.dp, vertical = 4.dp)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val iconRes = if (index == 0) {
                    any.ui.common.R.drawable.ic_baseline_home_24
                } else {
                    any.ui.common.R.drawable.ic_baseline_folder_24
                }
                val tintColor = if (isCurrent) {
                    MaterialTheme.colors.onPrimary
                } else {
                    MaterialTheme.colors.onBackground
                }
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = "",
                    modifier = Modifier.size(20.dp),
                    tint = tintColor,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (index != 0) item else stringResource(R.string.root_folder),
                    color = tintColor,
                )
            }

            if (index != segments.lastIndex) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Arrow right",
                )
            }
        }
    }
}
