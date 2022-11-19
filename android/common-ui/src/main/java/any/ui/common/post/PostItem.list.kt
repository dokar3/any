package any.ui.common.post

import any.base.R as BaseR
import android.content.res.Resources
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalMinimumTouchTargetEnforcement
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.ImmutableHolder
import any.base.util.compose.performLongPress
import any.data.entity.Post
import any.data.entity.ServiceViewType
import any.domain.entity.UiPost
import any.richtext.isNullOrEmpty
import any.ui.common.richtext.RichText
import any.ui.common.theme.divider
import any.ui.common.theme.imagePlaceholder
import any.ui.common.theme.sizes
import any.ui.common.theme.thumb
import any.ui.common.theme.thumbBorder
import any.ui.common.widget.Avatar
import any.ui.common.widget.CollectButton
import any.ui.common.widget.CommentsButton

private const val INFO_ANNOTATION_AUTHOR = "author"

@Composable
fun ListPostItem(
    onCommentsClick: ((UiPost) -> Unit)?,
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
    showDivider: Boolean = true,
    titleTextColor: Color = MaterialTheme.colors.onBackground,
    avatarSize: Dp = 32.dp,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val thumbnail = post.media?.firstOrNull()
    if (thumbnail == null) {
        TextListPostItem(
            post = post,
            modifier = modifier,
            onCommentsClick = onCommentsClick,
            onCollectClick = onCollectClick,
            onMoreClick = onMoreClick,
            onUserClick = onUserClick,
            onLinkClick = onLinkClick,
            onClick = onClick,
            onLongClick = onLongClick,
            defThumbAspectRatio = defThumbAspectRatio,
            titleTextColor = titleTextColor,
            avatarSize = avatarSize,
            showDivider = showDivider,
            showCollectButton = showCollectButton,
            showMoreButton = showMoreButton,
            padding = padding,
            interactionSource = interactionSource,
        )
    } else {
        CoverListPostItem(
            post = post,
            thumbnail = thumbnail,
            defThumbAspectRatio = defThumbAspectRatio,
            modifier = modifier,
            onCommentsClick = onCommentsClick,
            onCollectClick = onCollectClick,
            onMoreClick = onMoreClick,
            onUserClick = onUserClick,
            onLinkClick = onLinkClick,
            onClick = onClick,
            onLongClick = onLongClick,
            titleTextColor = titleTextColor,
            avatarSize = avatarSize,
            showCollectButton = showCollectButton,
            showMoreButton = showMoreButton,
            showDivider = showDivider,
            padding = padding,
            interactionSource = interactionSource,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TextListPostItem(
    onCommentsClick: ((UiPost) -> Unit)?,
    onCollectClick: ((UiPost) -> Unit)?,
    onMoreClick: ((UiPost) -> Unit)?,
    onUserClick: ((userId: String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onClick: ((UiPost) -> Unit)?,
    onLongClick: ((UiPost) -> Unit)?,
    post: UiPost,
    defThumbAspectRatio: Float?,
    modifier: Modifier = Modifier,
    titleTextColor: Color,
    avatarSize: Dp,
    showCollectButton: Boolean,
    showMoreButton: Boolean,
    showDivider: Boolean,
    padding: PaddingValues,
    interactionSource: MutableInteractionSource,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                onClick = { onClick?.invoke(post) },
                onLongClick = {
                    hapticFeedback.performLongPress()
                    onLongClick?.invoke(post)
                }
            )
            .padding(padding)
            .let {
                if (showDivider) {
                    it
                        .drawPostDivider()
                        .padding(top = 16.dp, bottom = 8.dp)
                } else {
                    it
                }
            },
    ) {
        if (post.title.isNotEmpty()) {
            Text(
                text = post.title,
                color = titleTextColor,
                fontSize = 18.sp,
                maxLines = PostItemDefaults.TextMaxLines,
                overflow = TextOverflow.Ellipsis,
            )
        } else if (!post.summary.isNullOrEmpty()) {
            RichText(
                content = post.summary!!,
                onLinkClick = { onLinkClick?.invoke(it) },
                onTextClick = { onClick?.invoke(post) },
                onTextLongClick = { onLongClick?.invoke(post) },
                lineHeight = PostItemDefaults.PrimaryTextLineHeight,
                blockMaxLines = PostItemDefaults.TextMaxLines,
                blockTextOverflow = TextOverflow.Ellipsis,
                interactionSource = interactionSource,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val reference = post.reference
        if (reference != null) {
            ListPostItem(
                onCommentsClick = onCommentsClick,
                onCollectClick = onCollectClick,
                onMoreClick = onMoreClick,
                onUserClick = onUserClick,
                onLinkClick = onLinkClick,
                onClick = onClick,
                onLongClick = onLongClick,
                post = reference.post,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.06f),
                        shape = MaterialTheme.shapes.small,
                    ),
                defThumbAspectRatio = defThumbAspectRatio,
                showCollectButton = false,
                showMoreButton = false,
                showDivider = false,
                avatarSize = 24.dp,
                padding = PaddingValues(8.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        PostInfo(
            post = post,
            onCommentsClick = { onCommentsClick?.invoke(post) },
            onCollectClick = { onCollectClick?.invoke(post) },
            onMoreClick = { onMoreClick?.invoke(post) },
            onUserClick = { post.authorId?.let { onUserClick?.invoke(it) } },
            onOtherTextClick = { onClick?.invoke(post) },
            showCollectButton = showCollectButton,
            showMoreButton = showMoreButton,
            avatarSize = avatarSize,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CoverListPostItem(
    onCommentsClick: ((UiPost) -> Unit)?,
    onCollectClick: ((UiPost) -> Unit)?,
    onMoreClick: ((UiPost) -> Unit)?,
    onUserClick: ((userId: String) -> Unit)?,
    onLinkClick: ((String) -> Unit)?,
    onClick: ((UiPost) -> Unit)?,
    onLongClick: ((UiPost) -> Unit)?,
    post: UiPost,
    thumbnail: UiPost.Media,
    defThumbAspectRatio: Float?,
    modifier: Modifier = Modifier,
    titleTextColor: Color,
    avatarSize: Dp,
    showCollectButton: Boolean,
    showMoreButton: Boolean,
    showDivider: Boolean,
    padding: PaddingValues,
    interactionSource: MutableInteractionSource,
) {
    val hapticFeedback = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .combinedClickable(
                interactionSource = interactionSource,
                indication = rememberRipple(),
                onClick = { onClick?.invoke(post) },
                onLongClick = {
                    hapticFeedback.performLongPress()
                    onLongClick?.invoke(post)
                }
            )
            .padding(padding)
            .let {
                if (showDivider) {
                    it
                        .drawPostDivider()
                        .padding(top = 16.dp, bottom = 8.dp)
                } else {
                    it
                }
            },
    ) {
        Row(modifier = Modifier.padding(bottom = 8.dp)) {
            MediaPreview(
                media = ImmutableHolder(listOf(thumbnail)),
                viewType = ServiceViewType.List,
                defaultAspectRatio = defThumbAspectRatio,
                modifier = Modifier
                    .width(120.dp)
                    .shadow(MaterialTheme.sizes.thumbElevation)
                    .clip(MaterialTheme.shapes.thumb)
                    .background(MaterialTheme.colors.imagePlaceholder)
                    .border(
                        width = MaterialTheme.sizes.thumbBorderStroke,
                        color = MaterialTheme.colors.thumbBorder,
                        shape = MaterialTheme.shapes.thumb,
                    ),
                tagMargin = 6.dp,
                tagFontSize = 13.sp,
            )

            Spacer(modifier = Modifier.width(16.dp))

            if (post.title.isNotEmpty()) {
                Text(
                    text = post.title,
                    color = titleTextColor,
                    fontSize = 18.sp,
                    maxLines = PostItemDefaults.TextMaxLines,
                    overflow = TextOverflow.Ellipsis,
                )
            } else if (!post.summary.isNullOrEmpty()) {
                RichText(
                    content = post.summary!!,
                    onLinkClick = { onLinkClick?.invoke(it) },
                    onTextClick = { onClick?.invoke(post) },
                    onTextLongClick = { onLongClick?.invoke(post) },
                    lineHeight = PostItemDefaults.PrimaryTextLineHeight,
                    blockMaxLines = PostItemDefaults.TextMaxLines,
                    blockTextOverflow = TextOverflow.Ellipsis,
                    interactionSource = interactionSource,
                )
            }
        }

        val reference = post.reference
        if (reference != null) {
            ListPostItem(
                onCommentsClick = onCommentsClick,
                onCollectClick = onCollectClick,
                onMoreClick = onMoreClick,
                onUserClick = onUserClick,
                onLinkClick = onLinkClick,
                onClick = onClick,
                onLongClick = onLongClick,
                post = reference.post,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.06f),
                        shape = MaterialTheme.shapes.small,
                    ),
                defThumbAspectRatio = defThumbAspectRatio,
                showCollectButton = false,
                showMoreButton = false,
                showDivider = false,
                avatarSize = 24.dp,
                padding = PaddingValues(8.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))
        }

        PostInfo(
            post = post,
            onCommentsClick = { onCommentsClick?.invoke(post) },
            onCollectClick = { onCollectClick?.invoke(post) },
            onMoreClick = { onMoreClick?.invoke(post) },
            onUserClick = { post.authorId?.let { onUserClick?.invoke(it) } },
            onOtherTextClick = { onClick?.invoke(post) },
            showCollectButton = showCollectButton,
            showMoreButton = showMoreButton,
            avatarSize = avatarSize,
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun PostInfo(
    post: UiPost,
    onCommentsClick: (() -> Unit)?,
    onCollectClick: (() -> Unit)?,
    onMoreClick: (() -> Unit)?,
    onUserClick: (() -> Unit)?,
    onOtherTextClick: (() -> Unit)?,
    avatarSize: Dp,
    showCollectButton: Boolean,
    showMoreButton: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val author = post.author
        val avatar = post.avatar
        if (author != null) {
            Avatar(
                name = author,
                url = avatar,
                size = avatarSize,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onUserClick?.invoke() },
                )
            )

            Spacer(modifier = Modifier.width(8.dp))
        }

        val textColor = MaterialTheme.colors.onBackground
        var infoTextLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
        val context = LocalContext.current
        val info = remember(post, textColor) {
            buildPosInfo(
                post = post,
                secondaryTextColor = textColor.copy(alpha = 0.6f),
                resources = context.resources,
            )
        }
        Text(
            text = info,
            modifier = Modifier
                .weight(1f)
                .pointerInput(info) {
                    if (post.author.isNullOrEmpty()) {
                        return@pointerInput
                    }
                    detectTapGestures(
                        onTap = { pos ->
                            val layoutResult = infoTextLayoutResult
                            if (layoutResult == null) {
                                onOtherTextClick?.invoke()
                                return@detectTapGestures
                            }
                            val idx = layoutResult.getOffsetForPosition(pos)
                            val authorAnnotations = info.getStringAnnotations(
                                tag = INFO_ANNOTATION_AUTHOR,
                                start = idx,
                                end = idx,
                            )
                            if (authorAnnotations.isNotEmpty()) {
                                onUserClick?.invoke()
                            } else {
                                onOtherTextClick?.invoke()
                            }
                        }
                    )
                },
            style = LocalTextStyle.current.copy(fontSize = 14.sp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { infoTextLayoutResult = it },
            inlineContent = PostItemDefaults.postInfoLineContent,
        )

        CompositionLocalProvider(
            LocalMinimumTouchTargetEnforcement provides false,
            LocalContentAlpha provides 0.7f,
        ) {
            Row {
                val iconSize = 42.dp
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
                    IconButton(
                        onClick = { onMoreClick?.invoke() },
                        modifier = Modifier
                            .size(iconSize)
                            .alpha(PostItemDefaults.IconButtonsOpacity),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(BaseR.string.more_options),
                        )
                    }
                }
            }
        }
    }
}

private fun Modifier.drawPostDivider(
    height: Dp = 0.7.dp,
): Modifier = composed {
    val dividerColor = MaterialTheme.colors.divider
    drawWithContent {
        drawContent()
        val heightPx = height.toPx()
        drawLine(
            color = dividerColor,
            start = Offset(0f, size.height - heightPx),
            end = Offset(size.width, size.height - heightPx),
            strokeWidth = heightPx,
        )
    }
}

private fun buildPosInfo(
    post: UiPost,
    secondaryTextColor: Color,
    resources: Resources,
): AnnotatedString = buildAnnotatedString {
    val separator = " | "

    val author = post.author
    if (author != null) {
        addStringAnnotation(
            tag = INFO_ANNOTATION_AUTHOR,
            annotation = "",
            start = length,
            end = length + author.length,
        )
        append(author)
    }

    withStyle(style = SpanStyle(color = secondaryTextColor)) {
        val ref = post.reference
        if (ref != null) {
            if (length != 0) {
                append('\n')
            }
            appendInlineContent(ref.type.name)
            val text = when (ref.type) {
                Post.Reference.Type.Repost -> resources.getString(BaseR.string.repost)
                Post.Reference.Type.Quote -> resources.getString(BaseR.string.quote)
                Post.Reference.Type.Reply -> resources.getString(BaseR.string.reply)
            }
            append(text)
        }

        if (post.category != null) {
            if (length != 0) {
                append(separator)
            }
            append(post.category!!)
        }

        if (!post.date.isNullOrEmpty()) {
            if (length != 0) {
                append(separator)
            }
            append(post.date!!)
        }
    }
}
