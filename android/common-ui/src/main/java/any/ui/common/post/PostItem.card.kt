package any.ui.common.post

import any.base.R as BaseR
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.ImmutableHolder
import any.base.util.compose.performLongPress
import any.data.entity.PostsViewType
import any.domain.entity.UiPost
import any.richtext.isNullOrEmpty
import any.ui.common.richtext.RichText
import any.ui.common.theme.thumbBorder
import any.ui.common.widget.Avatar
import any.ui.common.widget.CollectButton
import any.ui.common.widget.CommentsButton

// Inspired by https://blog.jetbrains.com/
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardPostItem(
    onCommentsClick: ((UiPost) -> Unit)?,
    onCollectClick: ((UiPost) -> Unit)?,
    onMoreClick: ((UiPost) -> Unit)?,
    onMediaClick: ((post: UiPost, index: Int) -> Unit)?,
    onUserClick: ((userId: String) -> Unit)?,
    onClick: ((UiPost) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onLongClick: ((UiPost) -> Unit)?,
    post: UiPost,
    defThumbAspectRatio: Float?,
    modifier: Modifier = Modifier,
    showCollectButton: Boolean = true,
    showMoreButton: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(
        start = 16.dp,
        top = 16.dp,
        end = 16.dp,
    ),
    borderColor: Color = MaterialTheme.colors.thumbBorder,
    avatarSize: Dp = 36.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val hapticFeedback = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .padding(contentPadding)
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            )
            .combinedClickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                onClick = { onClick?.invoke(post) },
                onLongClick = {
                    hapticFeedback.performLongPress()
                    onLongClick?.invoke(post)
                },
            ),
    ) {
        val media = post.media
        if (!media.isNullOrEmpty()) {
            MediaPreview(
                media = ImmutableHolder(media),
                viewType = PostsViewType.Card,
                defaultAspectRatio = defThumbAspectRatio,
                onClick = { onMediaClick?.invoke(post, it) },
            )
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            val context = LocalContext.current
            val info = remember(post) {
                PostItemDefaults.buildPostInfo(post, context.resources)
            }
            val showInfo = info.isNotEmpty()
            if (showInfo) {
                Text(
                    text = info,
                    style = LocalTextStyle.current.copy(
                        fontSize = 14.sp,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
                    ),
                    maxLines = PostItemDefaults.TextMaxLines,
                    overflow = TextOverflow.Ellipsis,
                    inlineContent = PostItemDefaults.postInfoLineContent,
                )
            }

            val showTitle = post.title.isNotEmpty() && post.title.isNotBlank()
            if (showTitle) {
                if (showInfo) {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = post.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = PostItemDefaults.PrimaryTextLineHeight,
                    maxLines = PostItemDefaults.TextMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            val summary = post.summary
            if (!summary.isNullOrEmpty()) {
                if (showInfo || showTitle) {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                RichText(
                    content = summary,
                    onLinkClick = { onLinkClick?.invoke(it) },
                    onTextClick = { onClick?.invoke(post) },
                    onTextLongClick = { onLongClick?.invoke(post) },
                    interactionSource = interactionSource,
                    lineHeight = PostItemDefaults.PrimaryTextLineHeight,
                    blockMaxLines = PostItemDefaults.TextMaxLines,
                    blockTextOverflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            BottomBar(
                post = post,
                onCommentsClick = { onCommentsClick?.invoke(post) },
                onCollectClick = { onCollectClick?.invoke((post)) },
                onMoreClick = { onMoreClick?.invoke(post) },
                onUserClick = { post.authorId?.let { onUserClick?.invoke(it) } },
                avatarSize = avatarSize,
                showCollectButton = showCollectButton,
                showMoreButton = showMoreButton,
            )
        }

        val reference = post.reference
        if (reference != null) {
            CardPostItem(
                onCommentsClick = onCommentsClick,
                onCollectClick = onCollectClick,
                onMoreClick = onMoreClick,
                onMediaClick = onMediaClick,
                onUserClick = onUserClick,
                onClick = onClick,
                onLinkClick = onLinkClick,
                onLongClick = onLongClick,
                post = reference.post,
                defThumbAspectRatio = defThumbAspectRatio,
                modifier = Modifier
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp,
                    )
                    .background(
                        MaterialTheme.colors.onBackground.copy(alpha = 0.06f),
                        shape = MaterialTheme.shapes.medium,
                    ),
                avatarSize = 28.dp,
                showCollectButton = false,
                showMoreButton = false,
                borderColor = Color.Transparent,
                contentPadding = PaddingValues(0.dp),
            )
        }
    }
}

@Composable
private fun BottomBar(
    post: UiPost,
    onCommentsClick: (() -> Unit)?,
    onCollectClick: (() -> Unit)?,
    onMoreClick: (() -> Unit)?,
    onUserClick: (() -> Unit)?,
    avatarSize: Dp,
    modifier: Modifier = Modifier,
    showCollectButton: Boolean = true,
    showMoreButton: Boolean = true,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onUserClick?.invoke() },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val author = post.author
            val avatar = post.avatar
            if (author != null) {
                Avatar(name = author, url = avatar, size = avatarSize)

                Spacer(modifier = Modifier.width(16.dp))
            }

            Text(
                text = author ?: "",
                modifier = Modifier.wrapContentWidth(),
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val iconSize = 36.dp
            if (post.commentsKey != null) {
                CommentsButton(
                    onClick = { onCommentsClick?.invoke() },
                    commentCount = post.commentCount,
                    modifier = Modifier.size(iconSize),
                    iconAlpha = PostItemDefaults.IconButtonsOpacity,
                )
            }

            if (showCollectButton) {
                CollectButton(
                    isCollected = post.isCollected(),
                    onClick = { onCollectClick?.invoke() },
                    uncollectedIconAlpha = PostItemDefaults.IconButtonsOpacity,
                    size = iconSize,
                )
            }

            if (showMoreButton) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(BaseR.string.more_options),
                    modifier = Modifier
                        .size(iconSize)
                        .padding((iconSize - 24.dp) / 2)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false),
                            onClick = { onMoreClick?.invoke() },
                        )
                        .alpha(PostItemDefaults.IconButtonsOpacity),
                )
            }
        }
    }
}