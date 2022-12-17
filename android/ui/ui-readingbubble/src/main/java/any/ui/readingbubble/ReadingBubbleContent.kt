package any.ui.readingbubble

import any.base.R as BaseR
import any.ui.common.R as CommonUiR
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.image.ImageRequest
import any.ui.common.image.AsyncImage
import any.ui.common.modifier.verticalScrollBar
import any.ui.common.theme.imagePlaceholder
import any.ui.common.theme.secondaryText
import any.ui.common.theme.sizes
import any.ui.common.theme.thumb
import any.ui.common.theme.thumbBorder
import any.ui.common.widget.EmojiEmptyContent
import any.ui.readingbubble.entity.ReadingPost

@Composable
internal fun ReadingBubbleContent(
    onPostClick: (ReadingPost) -> Unit,
    onRemovePost: (ReadingPost) -> Unit,
    onClearPosts: () -> Unit,
    uiState: ReadingBubbleUiState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        TitleBar(
            showClearAll = uiState.posts.isNotEmpty(),
            onClearAllClick = onClearPosts,
        )

        if (uiState.posts.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScrollBar(state = listState),
                state = listState,
            ) {
                items(count = uiState.posts.size) {
                    val post = uiState.posts[uiState.posts.lastIndex - it]
                    ReadingItem(
                        post = post,
                        onClick = { onPostClick(post) },
                        onRemoveClick = { onRemovePost(post) },
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                EmojiEmptyContent {
                    Text(stringResource(BaseR.string.nothing_here))
                }
            }
        }
    }
}

@Composable
private fun ReadingItem(
    post: ReadingPost,
    onClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.thumb)
                .background(MaterialTheme.colors.imagePlaceholder)
                .border(
                    width = MaterialTheme.sizes.thumbBorderStroke,
                    color = MaterialTheme.colors.thumbBorder,
                    shape = MaterialTheme.shapes.thumb,
                ),
        ) {
            if (post.thumbnail != null) {
                AsyncImage(
                    request = ImageRequest.Url(post.thumbnail),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = post.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(modifier = Modifier.height(8.dp))

            val info = remember(post) {
                buildAnnotatedString {
                    val source = post.source
                    if (!source.isNullOrEmpty()) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(source)
                        }
                        append(' ')
                    }

                    val author = post.author
                    if (!author.isNullOrEmpty()) {
                        append(author)
                        append(' ')
                    }

                    val summary = post.summary
                    if (!summary.isNullOrEmpty()) {
                        append(summary)
                    }
                }
            }
            Text(
                text = info,
                color = MaterialTheme.colors.secondaryText,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = onRemoveClick) {
            Icon(
                painter = painterResource(CommonUiR.drawable.ic_baseline_close_24),
                contentDescription = stringResource(BaseR.string.remove),
                modifier = Modifier
                    .size(22.dp)
                    .alpha(0.7f)
                    .background(
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.1f),
                        shape = CircleShape,
                    )
                    .padding(2.dp),
            )
        }
    }
}
