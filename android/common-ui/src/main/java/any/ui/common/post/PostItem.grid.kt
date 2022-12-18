package any.ui.common.post

import any.base.R as BaseR
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.compose.ImmutableHolder
import any.base.util.compose.performLongPress
import any.data.ThumbAspectRatio
import any.data.entity.Post
import any.data.entity.PostsViewType
import any.domain.entity.UiPost
import any.richtext.isNullOrEmpty
import any.ui.common.rememberScaleIndication
import any.ui.common.richtext.RichText
import any.ui.common.richtext.RichTextStyle
import any.ui.common.theme.imagePlaceholder
import any.ui.common.theme.sizes
import any.ui.common.theme.thumb
import any.ui.common.theme.thumbBorder
import any.ui.common.widget.Avatar
import any.ui.common.widget.CollectButton

@Composable
fun GridPostItem(
    onCollectClick: ((UiPost) -> Unit)?,
    onMoreClick: ((UiPost) -> Unit)?,
    onUserClick: ((userId: String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onClick: ((UiPost) -> Unit)?,
    onLongClick: ((UiPost) -> Unit)?,
    post: UiPost,
    defThumbAspectRatio: Float?,
    modifier: Modifier = Modifier,
    showCollectButton: Boolean = true,
    showMoreButton: Boolean = true,
    avatarSize: Dp = 32.dp,
    titleTextColor: Color = MaterialTheme.colors.onBackground,
    textItemContentBackgroundColor: Color = MaterialTheme.colors.primary.copy(alpha = 0.06f),
    nonMediaContentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val displayPost: UiPost
    val repostedBy: RepostUser?
    val reference = post.reference
    if (reference != null && reference.type == Post.Reference.Type.Repost) {
        displayPost = reference.post
        repostedBy = RepostUser(
            id = post.authorId,
            name = post.author,
        )
    } else {
        displayPost = post
        repostedBy = null
    }
    GridPostItemImpl(
        onCollectClick = onCollectClick,
        onMoreClick = onMoreClick,
        onUserClick = onUserClick,
        onLinkClick = onLinkClick,
        onClick = onClick,
        onLongClick = onLongClick,
        post = displayPost,
        repostedBy = repostedBy,
        defThumbAspectRatio = defThumbAspectRatio,
        modifier = modifier,
        showCollectButton = showCollectButton,
        showMoreButton = showMoreButton,
        avatarSize = avatarSize,
        titleTextColor = titleTextColor,
        textItemContentBackgroundColor = textItemContentBackgroundColor,
        nonMediaContentPadding = nonMediaContentPadding,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GridPostItemImpl(
    onCollectClick: ((UiPost) -> Unit)?,
    onMoreClick: ((UiPost) -> Unit)?,
    onUserClick: ((userId: String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onClick: ((UiPost) -> Unit)?,
    onLongClick: ((UiPost) -> Unit)?,
    post: UiPost,
    repostedBy: RepostUser?,
    defThumbAspectRatio: Float?,
    modifier: Modifier = Modifier,
    showCollectButton: Boolean = true,
    showMoreButton: Boolean = true,
    avatarSize: Dp = 32.dp,
    titleTextColor: Color = MaterialTheme.colors.onBackground,
    textItemContentBackgroundColor: Color = MaterialTheme.colors.primary.copy(alpha = 0.06f),
    nonMediaContentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val thumbnail = post.media?.firstOrNull()
    if (thumbnail == null) {
        TextGridPostItem(
            onCollectClick = onCollectClick,
            onMoreClick = onMoreClick,
            onUserClick = onUserClick,
            onLinkClick = onLinkClick,
            onClick = onClick,
            onLongClick = onLongClick,
            post = post,
            repostedBy = repostedBy,
            defThumbAspectRatio = defThumbAspectRatio,
            modifier = modifier,
            showCollectButton = showCollectButton,
            showMoreButton = showMoreButton,
            avatarSize = avatarSize,
            contentBackgroundColor = textItemContentBackgroundColor,
            nonMediaContentPadding = nonMediaContentPadding,
        )
    } else {
        CoverGridPostItem(
            onCollectClick = onCollectClick,
            onMoreClick = onMoreClick,
            onUserClick = onUserClick,
            onLinkClick = onLinkClick,
            onClick = onClick,
            onLongClick = onLongClick,
            post = post,
            repostedBy = repostedBy,
            thumbnail = thumbnail,
            defThumbAspectRatio = defThumbAspectRatio,
            modifier = modifier,
            titleTextColor = titleTextColor,
            showCollectButton = showCollectButton,
            showMoreButton = showMoreButton,
            avatarSize = avatarSize,
            nonMediaContentPadding = nonMediaContentPadding,
        )
    }
}

@ExperimentalFoundationApi
@Composable
private fun TextGridPostItem(
    onCollectClick: ((UiPost) -> Unit)?,
    onMoreClick: ((UiPost) -> Unit)?,
    onUserClick: ((userId: String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onClick: ((UiPost) -> Unit)?,
    onLongClick: ((UiPost) -> Unit)?,
    post: UiPost,
    repostedBy: RepostUser?,
    defThumbAspectRatio: Float?,
    modifier: Modifier = Modifier,
    showCollectButton: Boolean,
    showMoreButton: Boolean,
    avatarSize: Dp,
    contentBackgroundColor: Color,
    nonMediaContentPadding: PaddingValues,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = rememberScaleIndication(),
                onClick = { onClick?.invoke(post) },
                onLongClick = {
                    hapticFeedback.performLongPress()
                    onLongClick?.invoke(post)
                }
            ),
    ) {
        if (repostedBy != null) {
            RepostHeader(
                onUserClick = {
                    if (onUserClick != null && repostedBy.id != null) {
                        onUserClick(repostedBy.id)
                    }
                },
                username = repostedBy.name ?: "",
                contentPadding = PaddingValues(vertical = 8.dp),
            )
        }

        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.thumb)
                .background(contentBackgroundColor),
        ) {
            val aspectRatio = defThumbAspectRatio
                ?: ThumbAspectRatio.defaultAspectRatio(PostsViewType.Grid)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
            )

            Column {
                val reference = post.reference
                if (reference != null) {
                    GridPostItem(
                        onCollectClick = onCollectClick,
                        onMoreClick = onMoreClick,
                        onUserClick = onUserClick,
                        onLinkClick = onLinkClick,
                        onClick = onClick,
                        onLongClick = onLongClick,
                        post = reference.post,
                        defThumbAspectRatio = defThumbAspectRatio,
                        showCollectButton = false,
                        showMoreButton = false,
                        avatarSize = 26.dp,
                        textItemContentBackgroundColor = Color.Transparent,
                        nonMediaContentPadding = PaddingValues(horizontal = 8.dp),
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colors.onBackground.copy(alpha = 0.06f),
                                shape = MaterialTheme.shapes.small,
                            ),
                    )
                }

                val showTitle = post.title.isNotEmpty()
                if (showTitle) {
                    Text(
                        text = post.title,
                        modifier = Modifier.padding(8.dp),
                        fontWeight = FontWeight.Bold,
                        lineHeight = PostItemDefaults.PrimaryTextLineHeight,
                        maxLines = PostItemDefaults.TextMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (!post.summary.isNullOrEmpty()) {
                    if (showTitle) {
                        Divider(modifier = Modifier.padding(horizontal = 8.dp))
                    }

                    RichText(
                        content = post.summary!!,
                        modifier = Modifier.padding(8.dp),
                        onLinkClick = { onLinkClick?.invoke(it) },
                        onTextClick = { onClick?.invoke(post) },
                        onTextLongClick = { onLongClick?.invoke(post) },
                        style = RichTextStyle.Default,
                        lineHeight = PostItemDefaults.PrimaryTextLineHeight,
                        blockMaxLines = PostItemDefaults.TextMaxLines,
                        blockTextOverflow = TextOverflow.Ellipsis,
                        interactionSource = interactionSource,
                    )
                }
            }

        }

        BottomBar(
            post = post,
            onUserClick = onUserClick,
            onCollectClick = onCollectClick,
            onMoreClick = onMoreClick,
            showCollectButton = showCollectButton,
            showMoreButton = showMoreButton,
            avatarSize = avatarSize,
            bottomBarPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.padding(nonMediaContentPadding),
        )
    }
}

@ExperimentalFoundationApi
@Composable
private fun CoverGridPostItem(
    onCollectClick: ((UiPost) -> Unit)?,
    onMoreClick: ((UiPost) -> Unit)?,
    onUserClick: ((userId: String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onClick: ((UiPost) -> Unit)?,
    onLongClick: ((UiPost) -> Unit)?,
    post: UiPost,
    repostedBy: RepostUser?,
    thumbnail: UiPost.Media,
    defThumbAspectRatio: Float?,
    titleTextColor: Color,
    showCollectButton: Boolean,
    showMoreButton: Boolean,
    avatarSize: Dp,
    modifier: Modifier = Modifier,
    nonMediaContentPadding: PaddingValues,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = rememberScaleIndication(),
                onClick = {
                    onClick?.invoke(post)
                },
                onLongClick = {
                    hapticFeedback.performLongPress()
                    onLongClick?.invoke(post)
                }
            ),
    ) {
        if (repostedBy != null) {
            RepostHeader(
                onUserClick = {
                    if (onUserClick != null && repostedBy.id != null) {
                        onUserClick(repostedBy.id)
                    }
                },
                username = repostedBy.name ?: "",
                contentPadding = PaddingValues(vertical = 8.dp),
            )
        }

        val reference = post.reference
        if (reference != null) {
            GridPostItem(
                onCollectClick = onCollectClick,
                onMoreClick = onMoreClick,
                onUserClick = onUserClick,
                onLinkClick = onLinkClick,
                onClick = onClick,
                onLongClick = onLongClick,
                post = reference.post,
                defThumbAspectRatio = defThumbAspectRatio,
                showCollectButton = false,
                showMoreButton = false,
                avatarSize = 26.dp,
                textItemContentBackgroundColor = Color.Transparent,
                nonMediaContentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.06f),
                        shape = MaterialTheme.shapes.small,
                    ),
            )
        }

        MediaPreview(
            media = ImmutableHolder(listOf(thumbnail)),
            viewType = PostsViewType.Grid,
            defaultAspectRatio = defThumbAspectRatio,
            modifier = Modifier
                .shadow(MaterialTheme.sizes.thumbElevation)
                .clip(MaterialTheme.shapes.thumb)
                .background(MaterialTheme.colors.imagePlaceholder)
                .border(
                    width = MaterialTheme.sizes.thumbBorderStroke,
                    color = MaterialTheme.colors.thumbBorder,
                    shape = MaterialTheme.shapes.thumb,
                ),
            alignment = Alignment.Center,
            tagMargin = 12.dp,
        )

        if (post.title.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = post.title,
                modifier = Modifier.padding(nonMediaContentPadding),
                color = titleTextColor,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = PostItemDefaults.PrimaryTextLineHeight,
                maxLines = PostItemDefaults.TextMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        } else if (!post.summary.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))

            RichText(
                content = post.summary!!,
                onLinkClick = { onLinkClick?.invoke(it) },
                modifier = Modifier.padding(nonMediaContentPadding),
                onTextClick = { onClick?.invoke(post) },
                onTextLongClick = { onLongClick?.invoke(post) },
                fontSize = 14.sp,
                lineHeight = PostItemDefaults.PrimaryTextLineHeight,
                blockMaxLines = PostItemDefaults.TextMaxLines,
                blockTextOverflow = TextOverflow.Ellipsis,
                interactionSource = interactionSource,
            )
        }

        BottomBar(
            post = post,
            onUserClick = onUserClick,
            onCollectClick = onCollectClick,
            onMoreClick = onMoreClick,
            showCollectButton = showCollectButton,
            showMoreButton = showMoreButton,
            avatarSize = avatarSize,
            bottomBarPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.padding(nonMediaContentPadding),
        )
    }
}

@Composable
private fun BottomBar(
    post: UiPost,
    onUserClick: ((userId: String) -> Unit)?,
    onCollectClick: ((UiPost) -> Unit)?,
    onMoreClick: ((UiPost) -> Unit)?,
    showCollectButton: Boolean,
    showMoreButton: Boolean,
    avatarSize: Dp,
    bottomBarPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottomBarPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { post.authorId?.let { onUserClick?.invoke(it) } },
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val author = post.author
            val avatar = post.avatar
            if (author != null) {
                Avatar(name = author, url = avatar, size = avatarSize)

                Spacer(modifier = Modifier.width(8.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (!post.author.isNullOrEmpty()) {
                Text(
                    text = post.author!!,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        ActionButtons(
            post = post,
            onCollectClick = { onCollectClick?.invoke(post) },
            onMoreClick = { onMoreClick?.invoke(post) },
            modifier = Modifier,
            showCollectButton = showCollectButton,
            showMoreButton = showMoreButton,
        )
    }
}

@Composable
private fun ActionButtons(
    post: UiPost,
    modifier: Modifier = Modifier,
    onCollectClick: (() -> Unit)?,
    onMoreClick: (() -> Unit)?,
    showCollectButton: Boolean,
    showMoreButton: Boolean,
    buttonSize: Dp = 30.dp,
    alpha: Float = 0.7f,
) {
    CompositionLocalProvider(
        LocalContentAlpha provides alpha,
    ) {
        Row(modifier = modifier) {
            if (showCollectButton) {
                CollectButton(
                    isCollected = post.isCollected(),
                    onClick = { onCollectClick?.invoke() },
                    uncollectedIconAlpha = PostItemDefaults.IconButtonsOpacity,
                    size = buttonSize,
                )
            }

            if (showMoreButton) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(BaseR.string.more_options),
                    modifier = Modifier
                        .size(buttonSize)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = rememberRipple(bounded = false),
                            onClick = { onMoreClick?.invoke() },
                        )
                        .padding(4.dp)
                        .alpha(PostItemDefaults.IconButtonsOpacity),
                )
            }
        }
    }
}
