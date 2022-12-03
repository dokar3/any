package any.ui.common.post

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.compose.ImmutableHolder
import any.base.image.ImageRequest
import any.data.ThumbAspectRatio
import any.data.entity.Post
import any.data.entity.PostsViewType
import any.domain.entity.UiPost
import any.ui.common.image.AsyncImage
import any.ui.common.theme.imagePlaceholder
import any.ui.common.video.VideoView
import any.ui.common.video.rememberVideoPlaybackState
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.HorizontalPagerIndicator
import com.google.accompanist.pager.rememberPagerState

private const val MIN_ASPECT_RATIO = 1f / 1.618f

@Composable
internal fun MediaPreview(
    media: ImmutableHolder<List<UiPost.Media>>,
    viewType: PostsViewType,
    defaultAspectRatio: Float?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onClick: ((index: Int) -> Unit)? = null,
    contentScale: ContentScale = ContentScale.Crop,
    minAspectRatio: Float = MIN_ASPECT_RATIO,
    tagMargin: Dp = 16.dp,
    tagFontSize: TextUnit = 16.sp,
    tagFontWeight: FontWeight = FontWeight.Bold,
    fadeIn: Boolean = true,
    indicatorMargin: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
    indicatorPadding: PaddingValues = PaddingValues(8.dp),
    indicatorSize: Dp = 8.dp,
    showIndicator: Boolean = media.value.size > 1,
) {
    if (media.value.size == 1) {
        val item = media.value.first()
        val r = item.aspectRatio
            ?: defaultAspectRatio
            ?: ThumbAspectRatio.defaultAspectRatio(viewType)
        val aspectRatio = r.coerceAtLeast(minAspectRatio)
        MediaPreviewItem(
            media = item,
            aspectRatio = aspectRatio,
            modifier = modifier,
            contentDescription = contentDescription,
            onClick = onClick?.let { block -> { block(0) } },
            contentScale = contentScale,
            tagMargin = tagMargin,
            tagFontSize = tagFontSize,
            tagFontWeight = tagFontWeight,
            fadeIn = fadeIn,
        )
    } else {
        MediaPreviewPager(
            media = media,
            viewType = viewType,
            defaultAspectRatio = defaultAspectRatio,
            modifier = modifier,
            contentDescription = contentDescription,
            onClick = onClick,
            contentScale = contentScale,
            minAspectRatio = minAspectRatio,
            tagMargin = tagMargin,
            tagFontSize = tagFontSize,
            tagFontWeight = tagFontWeight,
            fadeIn = fadeIn,
            indicatorMargin = indicatorMargin,
            indicatorPadding = indicatorPadding,
            indicatorSize = indicatorSize,
            showIndicator = showIndicator,
        )
    }
}

@OptIn(ExperimentalPagerApi::class)
@Composable
private fun MediaPreviewPager(
    media: ImmutableHolder<List<UiPost.Media>>,
    viewType: PostsViewType,
    defaultAspectRatio: Float?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onClick: ((index: Int) -> Unit)? = null,
    contentScale: ContentScale = ContentScale.Crop,
    minAspectRatio: Float = MIN_ASPECT_RATIO,
    tagMargin: Dp = 16.dp,
    tagFontSize: TextUnit = 16.sp,
    tagFontWeight: FontWeight = FontWeight.Bold,
    fadeIn: Boolean = true,
    indicatorMargin: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
    indicatorPadding: PaddingValues = PaddingValues(8.dp),
    indicatorSize: Dp = 8.dp,
    showIndicator: Boolean = media.value.size > 1,
) {
    val r = media.value.firstOrNull()?.aspectRatio
        ?: defaultAspectRatio
        ?: ThumbAspectRatio.defaultAspectRatio(viewType)
    val aspectRatio = r.coerceAtLeast(minAspectRatio)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio),
    ) {
        val pagerState = rememberPagerState()
        HorizontalPager(
            count = media.value.size,
            state = pagerState,
        ) {
            MediaPreviewItem(
                media = media.value[it],
                aspectRatio = aspectRatio,
                modifier = modifier,
                contentDescription = contentDescription,
                onClick = onClick?.let { block -> { block(it) } },
                contentScale = contentScale,
                tagMargin = tagMargin,
                tagFontSize = tagFontSize,
                tagFontWeight = tagFontWeight,
                fadeIn = fadeIn,
            )
        }

        if (showIndicator) {
            HorizontalPagerIndicator(
                pagerState = pagerState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(indicatorMargin)
                    .background(
                        color = Color.Black.copy(alpha = 0.36f),
                        shape = CircleShape,
                    )
                    .padding(indicatorPadding),
                indicatorWidth = indicatorSize,
                indicatorHeight = indicatorSize,
                activeColor = Color.White,
                inactiveColor = Color.White.copy(alpha = 0.5f),
            )
        }
    }
}

@Composable
private fun MediaPreviewItem(
    media: UiPost.Media,
    aspectRatio: Float,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null,
    contentScale: ContentScale = ContentScale.Crop,
    tagMargin: Dp = 16.dp,
    tagFontSize: TextUnit = 16.sp,
    tagFontWeight: FontWeight = FontWeight.Bold,
    fadeIn: Boolean = true,
) {
    Box(modifier = modifier) {
        if (media.type == Post.Media.Type.Video && media.url.isNotEmpty()) {
            val playbackState = rememberVideoPlaybackState(url = media.url)
            VideoView(
                state = playbackState,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .background(MaterialTheme.colors.imagePlaceholder),
                thumbnail = media.thumbnail,
            )
        } else {
            AsyncImage(
                request = ImageRequest.Url(media.thumbnail ?: media.url),
                contentDescription = contentDescription,
                modifier = Modifier
                    .aspectRatio(aspectRatio)
                    .background(MaterialTheme.colors.imagePlaceholder)
                    .run {
                        if (onClick != null) {
                            clickable { onClick() }
                        } else {
                            this
                        }
                    },
                fadeIn = fadeIn,
                contentScale = contentScale,
            )
        }

        if (media.type == Post.Media.Type.Gif) {
            MediaTag(
                text = "GIF",
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(tagMargin, -tagMargin),
                fontSize = tagFontSize,
                fontWeight = tagFontWeight,
            )
        }
    }
}

@Composable
private fun MediaTag(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    backgroundColor: Color = Color.Black.copy(alpha = 0.72f),
    backgroundShape: Shape = MaterialTheme.shapes.small,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier
            .background(
                shape = backgroundShape,
                color = backgroundColor,
            )
            .padding(horizontal = 4.dp),
        color = textColor,
        fontSize = fontSize,
        fontWeight = fontWeight,
    )
}