package any.ui.common.post

import any.base.R as BaseR
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.material.Divider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.ImmutableHolder
import any.base.util.performLongPress
import any.data.entity.ServiceViewType
import any.domain.entity.UiPost
import any.richtext.isNullOrEmpty
import any.ui.common.richtext.RichText
import any.ui.common.theme.placeholder
import any.ui.common.widget.Avatar
import any.ui.common.widget.CollectButton
import any.ui.common.widget.CommentsButton

// Inspired by Youtube
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullWidthPostItem(
    onCommentsClick: ((UiPost) -> Unit)?,
    onCollectClick: ((UiPost) -> Unit)?,
    onMoreClick: ((UiPost) -> Unit)?,
    onMediaClick: ((post: UiPost, index: Int) -> Unit)?,
    onUserClick: ((userId: String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onClick: ((post: UiPost) -> Unit)?,
    onLongClick: ((post: UiPost) -> Unit)?,
    post: UiPost,
    defThumbAspectRatio: Float?,
    showCollectButton: Boolean,
    modifier: Modifier = Modifier,
    showMoreButton: Boolean = true,
    showDivider: Boolean = true,
    showOptionBar: Boolean = true,
    avatarSize: Dp = 36.dp,
    mediaPadding: PaddingValues = PaddingValues(0.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val spacing = 12.dp

    val hapticFeedback = LocalHapticFeedback.current

    val clickModifier = Modifier.combinedClickable(
        interactionSource = interactionSource,
        indication = rememberRipple(),
        onClick = { onClick?.invoke(post) },
        onLongClick = {
            hapticFeedback.performLongPress()
            onLongClick?.invoke(post)
        },
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(clickModifier),
    ) {
        val avatar = post.avatar

        val author = post.author

        val context = LocalContext.current
        val info = remember(post) { PostItemDefaults.buildPostInfo(post, context.resources) }

        val showInfoBar = !avatar.isNullOrEmpty() || !author.isNullOrEmpty() || info.isNotEmpty()
        if (showInfoBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = spacing,
                        top = spacing,
                        end = spacing,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (author != null) {
                    Avatar(
                        name = author,
                        url = avatar,
                        size = avatarSize,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { post.authorId?.let { onUserClick?.invoke(it) } }
                        ),
                    )

                    Spacer(modifier = Modifier.width(16.dp))
                }

                Column(modifier = Modifier.weight(1f)) {
                    if (!author.isNullOrEmpty()) {
                        Text(
                            text = author,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = PostItemDefaults.TextMaxLines,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { post.authorId?.let { onUserClick?.invoke(it) } }
                            )
                        )
                    }

                    if (info.isNotEmpty()) {
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
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (showMoreButton) {
                    MoreButton(onClick = { onMoreClick?.invoke(post) })
                }
            }
        }

        val showTitle = post.title.isNotEmpty() && post.title.isNotBlank()

        if (showTitle || !showInfoBar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showTitle) {
                    Text(
                        text = post.title,
                        modifier = Modifier.weight(1f),
                        fontWeight = FontWeight.Bold,
                        lineHeight = PostItemDefaults.PrimaryTextLineHeight,
                        maxLines = PostItemDefaults.TextMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (!showInfoBar) {
                    Spacer(modifier = Modifier.width(8.dp))

                    if (showMoreButton) {
                        MoreButton(onClick = { onMoreClick?.invoke(post) })
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(spacing))
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            val summary = post.summary
            if (!summary.isNullOrEmpty()) {
                RichText(
                    content = summary,
                    onLinkClick = { onLinkClick?.invoke(it) },
                    onTextClick = { onClick?.invoke(post) },
                    onTextLongClick = { onLongClick?.invoke(post) },
                    modifier = Modifier.padding(
                        start = spacing,
                        end = spacing,
                        bottom = spacing,
                    ),
                    interactionSource = interactionSource,
                    lineHeight = PostItemDefaults.PrimaryTextLineHeight,
                    blockMaxLines = PostItemDefaults.TextMaxLines,
                    blockTextOverflow = TextOverflow.Ellipsis,
                )
            }
            val media = post.media
            if (!media.isNullOrEmpty()) {
                MediaPreview(
                    media = ImmutableHolder(media),
                    viewType = ServiceViewType.FullWidth,
                    defaultAspectRatio = defThumbAspectRatio,
                    onClick = { onMediaClick?.invoke(post, it) },
                    modifier = Modifier.padding(mediaPadding),
                )
            }
            val reference = post.reference
            if (reference != null) {
                FullWidthPostItem(
                    post = reference.post,
                    defThumbAspectRatio = defThumbAspectRatio,
                    showCollectButton = showCollectButton,
                    onCommentsClick = onCommentsClick,
                    onCollectClick = onCollectClick,
                    onMoreClick = onMoreClick,
                    onMediaClick = onMediaClick,
                    onUserClick = onUserClick,
                    onLinkClick = onLinkClick,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    modifier = Modifier
                        .background(MaterialTheme.colors.placeholder),
                    showDivider = false,
                    showOptionBar = false,
                    showMoreButton = false,
                    avatarSize = 28.dp,
                    mediaPadding = PaddingValues(8.dp),
                )
            }
        }

        if (showOptionBar) {
            OptionBar(
                post = post,
                onCommentsClick = { onCommentsClick?.invoke(post) },
                onCollectClick = { onCollectClick?.invoke(post) },
                modifier = Modifier
                    .padding(
                        start = spacing,
                        top = 6.dp,
                        end = spacing,
                        bottom = 6.dp
                    ),
                showCollectButton = showCollectButton,
            )
        }

        if (showDivider) {
            Divider(thickness = 8.dp)
        }
    }
}

@Composable
private fun MoreButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = Icons.Default.MoreVert,
        contentDescription = stringResource(BaseR.string.more_options),
        modifier = modifier
            .size(24.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false),
                onClick = onClick,
            )
            .alpha(PostItemDefaults.IconButtonsOpacity),
    )
}

@Composable
private fun OptionBar(
    post: UiPost,
    onCommentsClick: (() -> Unit)?,
    onCollectClick: () -> Unit,
    modifier: Modifier = Modifier,
    showCollectButton: Boolean = true,
    iconSize: Dp = 32.dp,
) {
    if (post.commentsKey == null && !showCollectButton) {
        return
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (post.commentsKey != null) {
            CommentsButton(
                onClick = { onCommentsClick?.invoke() },
                commentCount = post.commentCount,
                modifier = Modifier.size(48.dp, iconSize),
                iconAlpha = PostItemDefaults.IconButtonsOpacity,
            )
        }

        if (showCollectButton) {
            CollectButton(
                isCollected = post.isCollected(),
                onClick = { onCollectClick() },
                size = iconSize,
                uncollectedIconAlpha = PostItemDefaults.IconButtonsOpacity,
            )
        }
    }
}