package any.ui.common.post

import any.base.R as BaseR
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import any.base.ImmutableHolder
import any.data.entity.ServiceViewType
import any.domain.entity.UiPost
import any.ui.common.modifier.gridItemPadding
import any.ui.common.modifier.rememberLazyGridColumnCount
import any.ui.common.theme.sizes
import any.ui.common.widget.ProgressBar
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull

internal enum class PostListContentType {
    Header,
    Empty,
    ListItem,
    GridItem,
    FullWidthItem,
    Footer,
}

private const val ITEM_KEY_LOAD_MORE = "[load_more]"

@OptIn(ExperimentalFoundationApi::class)
@NonRestartableComposable
@Composable
fun PostList(
    state: LazyGridState,
    posts: ImmutableHolder<List<UiPost>>,
    pageKey: ImmutableHolder<Any?>,
    viewType: ServiceViewType,
    defThumbAspectRatio: Float?,
    onCommentsClick: ((UiPost) -> Unit)?,
    onCollectClick: ((UiPost) -> Unit)?,
    onMoreClick: ((UiPost) -> Unit)?,
    onMediaClick: ((post: UiPost, index: Int) -> Unit)?,
    onUserClick: ((serviceId: String, userId: String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onItemClick: ((UiPost) -> Unit)?,
    onItemLongClick: ((UiPost) -> Unit)?,
    modifier: Modifier = Modifier,
    headerContent: @Composable (() -> Unit)? = null,
    emptyContent: @Composable (() -> Unit)? = null,
    loadMoreContent: @Composable (() -> Unit)? = null,
    itemGraphicsLayer: (GraphicsLayerScope.(Int) -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    showLoadMore: Boolean = loadMoreContent != null,
    isLoadingMore: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    PostListImpl(
        state = state,
        posts = posts,
        pageKey = pageKey,
        viewType = viewType,
        defThumbAspectRatio = defThumbAspectRatio,
        onCommentsClick = onCommentsClick,
        onCollectClick = onCollectClick,
        onMoreClick = onMoreClick,
        onMediaClick = onMediaClick,
        onUserClick = onUserClick,
        onLinkClick = onLinkClick,
        onItemClick = onItemClick,
        onItemLongClick = onItemLongClick,
        headerContent = headerContent,
        emptyContent = emptyContent,
        loadMoreContent = loadMoreContent,
        itemGraphicsLayer = itemGraphicsLayer,
        onLoadMore = onLoadMore,
        showLoadMore = showLoadMore,
        isLoadingMore = isLoadingMore,
        contentPadding = contentPadding,
        modifier = modifier,
    )
}

@ExperimentalFoundationApi
@Composable
private fun PostListImpl(
    state: LazyGridState,
    posts: ImmutableHolder<List<UiPost>>,
    pageKey: ImmutableHolder<Any?>,
    viewType: ServiceViewType,
    defThumbAspectRatio: Float?,
    onCommentsClick: ((UiPost) -> Unit)?,
    onCollectClick: ((UiPost) -> Unit)?,
    onMoreClick: ((UiPost) -> Unit)?,
    onMediaClick: ((post: UiPost, index: Int) -> Unit)?,
    onUserClick: ((serviceId: String, userId: String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onItemClick: ((UiPost) -> Unit)?,
    onItemLongClick: ((post: UiPost) -> Unit)?,
    headerContent: @Composable (() -> Unit)?,
    emptyContent: @Composable (() -> Unit)?,
    loadMoreContent: @Composable (() -> Unit)?,
    itemGraphicsLayer: (GraphicsLayerScope.(Int) -> Unit)?,
    onLoadMore: (() -> Unit)?,
    showLoadMore: Boolean,
    isLoadingMore: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val loadMore by rememberUpdatedState(newValue = onLoadMore)

    val isLoadingMoreState = rememberUpdatedState(newValue = isLoadingMore)

    LaunchedEffect(state, pageKey.value, posts) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo }
            .mapNotNull { it.lastOrNull() }
            .map { it.key == ITEM_KEY_LOAD_MORE }
            .filter { it }
            .distinctUntilChanged()
            .collect {
                if (!isLoadingMoreState.value) {
                    loadMore?.invoke()
                }
            }
    }

    val gridCells = GridCells.Adaptive(minSize = MaterialTheme.sizes.minAdaptiveGridCellsWidth)
    val cellSpacing = 16.dp
    val columnCount = rememberLazyGridColumnCount(
        state = state,
        cells = gridCells,
        spacing = cellSpacing,
    )

    LazyVerticalGrid(
        columns = gridCells,
        state = state,
        modifier = modifier,
        contentPadding = contentPadding,
    ) {
        if (headerContent != null) {
            item(
                key = "[header]",
                contentType = PostListContentType.Header,
                span = { GridItemSpan(maxLineSpan) },
            ) {
                headerContent()
            }
        }

        when (viewType) {
            ServiceViewType.Grid -> {
                itemsIndexed(
                    items = posts.value,
                    key = { _, post -> post.url },
                    contentType = { _, _ -> PostListContentType.GridItem },
                ) { index, post ->
                    GridPostItem(
                        onCollectClick = onCollectClick,
                        onMoreClick = onMoreClick,
                        onUserClick = { onUserClick?.invoke(post.serviceId, it) },
                        onLinkClick = onLinkClick,
                        onClick = onItemClick,
                        onLongClick = onItemLongClick,
                        post = post,
                        defThumbAspectRatio = defThumbAspectRatio,
                        modifier = Modifier
                            .gridItemPadding(
                                spacing = cellSpacing,
                                columnCount = columnCount,
                                index = index,
                            )
                            .graphicsLayer {
                                itemGraphicsLayer?.invoke(this, index)
                            },
                    )
                }
            }

            ServiceViewType.FullWidth -> {
                itemsIndexed(
                    items = posts.value,
                    key = { _, post -> post.url },
                    contentType = { _, _ -> PostListContentType.FullWidthItem },
                    span = { _, _ -> GridItemSpan(maxLineSpan) },
                ) { idx, post ->
                    FullWidthPostItem(
                        onCommentsClick = onCommentsClick,
                        onCollectClick = onCollectClick,
                        onMoreClick = onMoreClick,
                        onMediaClick = onMediaClick,
                        onUserClick = { onUserClick?.invoke(post.serviceId, it) },
                        onLinkClick = onLinkClick,
                        onClick = onItemClick,
                        onLongClick = onItemLongClick,
                        post = post,
                        defThumbAspectRatio = defThumbAspectRatio,
                        showCollectButton = true,
                        modifier = Modifier.graphicsLayer {
                            itemGraphicsLayer?.invoke(this, idx)
                        }
                    )
                }
            }

            ServiceViewType.Card -> {
                itemsIndexed(
                    items = posts.value,
                    key = { _, post -> post.url },
                    contentType = { _, _ -> PostListContentType.ListItem },
                    span = { _, _ -> GridItemSpan(maxLineSpan) },
                ) { idx, post ->
                    CardPostItem(
                        onCommentsClick = onCommentsClick,
                        onCollectClick = onCollectClick,
                        onMoreClick = onMoreClick,
                        onMediaClick = onMediaClick,
                        onUserClick = { onUserClick?.invoke(post.serviceId, it) },
                        onLinkClick = onLinkClick,
                        onClick = onItemClick,
                        onLongClick = onItemLongClick,
                        post = post,
                        defThumbAspectRatio = defThumbAspectRatio,
                        modifier = Modifier.graphicsLayer {
                            itemGraphicsLayer?.invoke(this, idx)
                        }
                    )
                }
            }

            ServiceViewType.List -> {
                itemsIndexed(
                    items = posts.value,
                    key = { _, post -> post.url },
                    contentType = { _, _ -> PostListContentType.ListItem },
                    span = { _, _ -> GridItemSpan(maxLineSpan) },
                ) { idx, post ->
                    ListPostItem(
                        onCommentsClick = onCommentsClick,
                        onCollectClick = onCollectClick,
                        onMoreClick = onMoreClick,
                        onUserClick = { onUserClick?.invoke(post.serviceId, it) },
                        onLinkClick = onLinkClick,
                        onClick = onItemClick,
                        onLongClick = onItemLongClick,
                        post = post,
                        defThumbAspectRatio = defThumbAspectRatio,
                        modifier = Modifier.graphicsLayer {
                            itemGraphicsLayer?.invoke(this, idx)
                        }
                    )
                }
            }
        }

        if (posts.value.isEmpty() && emptyContent != null) {
            item(
                key = "[empty]",
                contentType = PostListContentType.Empty,
                span = { GridItemSpan(maxLineSpan) },
            ) {
                emptyContent()
            }
        }

        // Add an empty full-span item for the scrollable pixels calculation, because
        // the result may be incorrect if items of the last row have different heights.
        item(
            span = { GridItemSpan(maxLineSpan) },
            content = {},
        )

        if (posts.value.isNotEmpty() &&
            showLoadMore &&
            onLoadMore != null &&
            loadMoreContent != null
        ) {
            item(
                key = ITEM_KEY_LOAD_MORE,
                contentType = PostListContentType.Footer,
                span = { GridItemSpan(maxLineSpan) },
            ) {
                loadMoreContent()
            }
        }
    }
}

@Composable
fun RetryItem(
    message: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(message)

        Spacer(modifier = Modifier.width(16.dp))

        OutlinedButton(onClick = onClick) {
            Text(stringResource(BaseR.string.retry))
        }
    }
}

@Composable
fun LoadingItem(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp, 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        ProgressBar()
    }
}