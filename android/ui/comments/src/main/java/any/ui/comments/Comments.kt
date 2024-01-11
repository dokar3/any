package any.ui.comments

import any.base.R as BaseR
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import any.base.compose.ImmutableHolder
import any.base.compose.StableHolder
import any.base.image.ImageRequest
import any.data.ThumbAspectRatio
import any.data.entity.Post
import any.domain.entity.UiPost
import any.domain.entity.UiServiceManifest
import any.navigation.NavEvent
import any.navigation.Routes
import any.navigation.imagePager
import any.ui.common.image.AsyncImage
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.theme.placeholder
import any.ui.common.theme.secondaryText
import any.ui.common.theme.sizes
import any.ui.common.theme.thumb
import any.ui.common.theme.thumbBorder
import any.ui.common.widget.Avatar
import any.ui.common.widget.BottomSheetTitle
import any.ui.common.widget.EmojiEmptyContent
import any.ui.common.widget.EndOfList
import any.ui.common.widget.ProgressBar
import any.ui.common.widget.UiMessagePopup
import com.dokar.sheets.BottomSheet
import com.dokar.sheets.BottomSheetState
import com.dokar.sheets.BottomSheetValue
import com.dokar.sheets.PeekHeight
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull

@Composable
fun CommentsSheet(
    onNavigate: (NavEvent) -> Unit,
    state: BottomSheetState,
    service: UiServiceManifest?,
    post: UiPost,
    modifier: Modifier = Modifier,
    viewModel: CommentsViewModel = viewModel(
        factory = CommentsViewModel.Factory(LocalContext.current)
    ),
) {
    BottomSheet(
        state = state,
        modifier = modifier.fillMaxSize(),
        peekHeight = PeekHeight.fraction(0.65f),
    ) {
        val uiState by viewModel.uiState.collectAsState()
        val comments = uiState.comments

        LaunchedEffect(service, post) {
            viewModel.resetIfNeeded(service, post.raw)
        }

        LaunchedEffect(viewModel, state, service, post, comments.size) {
            snapshotFlow { state.value }
                .distinctUntilChanged()
                .filter {
                    comments.isEmpty() &&
                            (state.value == BottomSheetValue.Peeked ||
                                    state.value == BottomSheetValue.Expanded)
                }
                .collect {
                    viewModel.fetchFirstPage(service, post.raw)
                    cancel()
                }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            BottomSheetTitle(
                text = stringResource(BaseR.string.comments),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { viewModel.fetchFirstPage(service, post.raw) },
                ),
                verticalPadding = 8.dp,
            )

            if (uiState.isLoading && !uiState.isLoadingMore) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ProgressBar()
                }
            }

            if (comments.isNotEmpty()) {
                CommentList(
                    onExpandReplies = { viewModel.expandReplies(it) },
                    onCollapseReplies = { viewModel.collapseReplies(it) },
                    comments = StableHolder(comments),
                    pageKey = ImmutableHolder(uiState.pageKey),
                    onImageClick = { comment, index ->
                        val images = comment.media
                        if (!images.isNullOrEmpty()) {
                            val navEvent = NavEvent.PushImagePager(
                                route = Routes.imagePager(
                                    title = comment.content,
                                    currPage = index
                                ),
                                images = images.map { it.url },
                            )
                            onNavigate(navEvent)
                        }
                    },
                    loadMoreEnabled = uiState.hasMore || uiState.isLoadingMore,
                    onLoadMore = {
                        viewModel.fetchMore(
                            service = service,
                            post = post.raw,
                        )
                    },
                )
            } else if (!uiState.isLoading && !uiState.isLoadingMore) {
                EmojiEmptyContent(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                ) {
                    Text(stringResource(BaseR.string.no_comments))
                }
            }
        }

        UiMessagePopup(
            message = uiState.message,
            onMessageDismissed = viewModel::removeMessageById,
            onRetry = viewModel::fetchPrevRequest,
        )
    }
}

@Composable
private fun CommentList(
    onExpandReplies: (UiComment.ExpandReplies) -> Unit,
    onCollapseReplies: (UiComment.CollapseReplies) -> Unit,
    comments: StableHolder<List<UiComment>>,
    pageKey: ImmutableHolder<Any?>,
    modifier: Modifier = Modifier,
    onImageClick: ((UiComment.Comment, Int) -> Unit)? = null,
    loadMoreEnabled: Boolean = true,
    onLoadMore: (() -> Unit)? = null,
    avatarSize: Dp = 40.dp,
) {
    val listState = rememberLazyListState()


    LaunchedEffect(listState, pageKey.value, loadMoreEnabled) {
        if (!loadMoreEnabled) {
            return@LaunchedEffect
        }
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .mapNotNull { it.lastOrNull()?.index }
            .distinctUntilChanged()
            .filter { it == listState.layoutInfo.totalItemsCount - 1 }
            .collect { onLoadMore?.invoke() }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .verticalScrollBar(listState),
        state = listState,
    ) {
        val commentList = comments.value
        itemsIndexed(
            items = commentList,
            key = { _, item ->
                when (item) {
                    is UiComment.Comment -> item.id.toString()
                    is UiComment.Reply -> item.id.toString()
                    is UiComment.CollapseReplies -> "collapse:${item.commentId}"
                    is UiComment.ExpandReplies -> "expand:${item.commentId}"
                }
            },
            contentType = { _, item -> item::class.simpleName },
        ) { idx, comment ->
            when (comment) {
                is UiComment.Comment -> {
                    CommentItem(
                        onImageClick = { onImageClick?.invoke(comment, it) },
                        comment = comment,
                        avatarSize = avatarSize,
                        showTopDivider = idx != 0,
                        padding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    )
                }

                is UiComment.Reply -> {
                    val isTheLastReply = when (idx) {
                        commentList.lastIndex -> true
                        else -> commentList[idx + 1] !is UiComment.Reply
                    }
                    ReplyItem(
                        reply = comment,
                        modifier = Modifier.padding(
                            start = avatarSize + 12.dp * 2,
                            end = 12.dp,
                        ),
                        contentPadding = PaddingValues(
                            bottom = if (isTheLastReply) 0.dp else 16.dp,
                        ),
                    )
                    Spacer(modifier = Modifier.height(if (isTheLastReply) 8.dp else 0.dp))
                }

                is UiComment.CollapseReplies -> {
                    CollapseRepliesItem(
                        onClick = { onCollapseReplies(comment) },
                        count = comment.count,
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 8.dp,
                        ),
                    )
                }

                is UiComment.ExpandReplies -> {
                    ExpandRepliesItem(
                        onClick = { onExpandReplies(comment) },
                        count = comment.count,
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 8.dp,
                        ),
                    )
                }
            }
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (loadMoreEnabled) {
                    ProgressBar(size = 24.dp)
                } else {
                    EndOfList(
                        onClick = { onLoadMore?.invoke() },
                    ) {
                        Text(stringResource(BaseR.string.no_more_comments))
                    }
                }
            }

        }
    }
}

@Composable
private fun CommentItem(
    onImageClick: (index: Int) -> Unit,
    comment: UiComment.Comment,
    avatarSize: Dp,
    modifier: Modifier = Modifier,
    showTopDivider: Boolean = true,
    lineHeight: TextUnit = 1.5.em,
    padding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showTopDivider) {
            Divider(
                thickness = 0.8.dp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.1f),
            )
        }

        Row(
            modifier = Modifier.padding(padding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Avatar(
                name = comment.username,
                url = comment.avatar,
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.placeholder),
                contentDescription = stringResource(BaseR.string.avatar),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                comment.username,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )

            Upvotes(upvotes = comment.upvotes)
        }

        val layoutDirection = LocalLayoutDirection.current
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = avatarSize + padding.calculateStartPadding(layoutDirection) * 2,
                    end = 8.dp,
                    bottom = 8.dp,
                )
        ) {
            val content = comment.content
            if (content.isNotEmpty()) {
                SelectionContainer {
                    Text(
                        text = content,
                        lineHeight = lineHeight,
                    )
                }
            }

            val images = comment.media
            if (!images.isNullOrEmpty()) {
                CommentImages(
                    images = ImmutableHolder(images),
                    onImageClick = onImageClick,
                )
            }
        }
    }
}

@Composable
private fun CommentImages(
    images: ImmutableHolder<List<Post.Media>>,
    modifier: Modifier = Modifier,
    onImageClick: ((index: Int) -> Unit)? = null,
) {
    val columns = when (images.value.size) {
        1 -> 1
        2 -> 2
        else -> 3
    }
    val chunked = remember(images) { images.value.chunked(columns) }
    Column(modifier = modifier.fillMaxWidth()) {
        for (i in chunked.indices) {
            val rowImages = chunked[i]
            Row(modifier = Modifier.fillMaxWidth()) {
                for (j in rowImages.indices) {
                    val image = rowImages[j]
                    val aspectRatio = if (columns == 1) {
                        ThumbAspectRatio.parseOrNull(image.aspectRatio) ?: 1f
                    } else {
                        1f
                    }
                    AsyncImage(
                        request = ImageRequest.Url(image.url),
                        contentDescription = stringResource(BaseR.string.comment_image),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(aspectRatio)
                            .padding(4.dp)
                            .clip(MaterialTheme.shapes.thumb)
                            .border(
                                width = MaterialTheme.sizes.thumbBorderStroke,
                                color = MaterialTheme.colors.thumbBorder,
                                shape = MaterialTheme.shapes.thumb,
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onImageClick?.invoke(i * columns + j) }
                            )
                    )
                }
                repeat(columns - rowImages.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ReplyItem(
    reply: UiComment.Reply,
    modifier: Modifier = Modifier,
    indentWidth: Dp = 16.dp,
    lineHeight: TextUnit = 1.5.em,
    indicatorColor: Color = MaterialTheme.colors.placeholder.copy(alpha = 0.5f),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val layoutDirection = LocalLayoutDirection.current
    val isLtr = layoutDirection == LayoutDirection.Ltr
    val usernameFontSize = 14.sp
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = indentWidth * (reply.depth - 1).coerceIn(0, 5))
            .drawBehind {
                val barWidth = 4.dp.toPx()
                val topLeft = Offset(if (isLtr) 0f else size.width - barWidth, 0f)
                drawRect(
                    color = indicatorColor,
                    topLeft = topLeft,
                    size = Size(4.dp.toPx(), size.height),
                )
            }
            .padding(start = 16.dp)
            .padding(contentPadding)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = reply.username,
                modifier = Modifier.weight(1f),
                fontSize = usernameFontSize,
                fontWeight = FontWeight.Bold,
            )

            Upvotes(upvotes = reply.upvotes)
        }

        Spacer(modifier = Modifier.height(4.dp))

        SelectionContainer {
            Text(
                text = reply.content,
                lineHeight = lineHeight,
            )
        }
    }
}

@Composable
private fun ExpandRepliesItem(
    onClick: () -> Unit,
    count: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(BaseR.string._show_more_comments, count),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colors.primary,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun CollapseRepliesItem(
    onClick: () -> Unit,
    count: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(BaseR.string._show_less_comments, count),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colors.primary,
        textAlign = TextAlign.End,
    )
}

@Composable
private fun Upvotes(
    upvotes: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val color = MaterialTheme.colors.secondaryText.copy(alpha = 0.3f)
        Text(
            upvotes.toString(),
            color = color,
            fontSize = 14.sp,
            maxLines = 1,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Default.ThumbUp,
            contentDescription = stringResource(BaseR.string.upvotes),
            modifier = Modifier.size(16.dp),
            tint = color,
        )
    }
}