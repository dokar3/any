package any.ui.post.item

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.image.ImageRequest
import any.base.util.compose.performLongPress
import any.domain.entity.UiContentElement
import any.ui.common.R
import any.ui.common.image.AsyncImage
import any.ui.common.theme.darkerImagePlaceholder
import any.ui.common.theme.imagePlaceholder
import any.ui.common.theme.secondaryText
import any.ui.common.theme.sizes
import any.ui.common.theme.thumb
import any.ui.common.video.VideoView
import any.ui.common.video.rememberVideoPlaybackState
import com.google.accompanist.pager.pagerTabIndicatorOffset
import kotlinx.coroutines.launch
import kotlin.math.min

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CarouselItem(
    carousel: UiContentElement.Carousel,
    onPlayVideoClick: (url: String) -> Unit,
    onImageClick: (url: String) -> Unit,
    onImageLongClick: (url: String) -> Unit,
    modifier: Modifier = Modifier,
    defaultAspectRatio: Float = 4 / 3f,
) {
    val borderColor = MaterialTheme.colors.onBackground.copy(alpha = 0.1f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = ItemsDefaults.ItemHorizontalSpacing,
                vertical = ItemsDefaults.ItemVerticalSpacing
            )
            .clip(MaterialTheme.shapes.thumb)
            .border(
                width = MaterialTheme.sizes.thumbBorderStroke,
                color = borderColor,
                shape = MaterialTheme.shapes.thumb,
            )
    ) {
        val scope = rememberCoroutineScope()

        val pageCount = carousel.items.size

        val pagerState = rememberPagerState(pageCount = { pageCount })

        val feedback = LocalHapticFeedback.current

        val evenColor = MaterialTheme.colors.imagePlaceholder
        val oddColor = MaterialTheme.colors.darkerImagePlaceholder

        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(carousel.aspectRatio ?: defaultAspectRatio)
                .background(MaterialTheme.colors.imagePlaceholder)
                .pointerInput(null) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (offset.x <= size.width / 5) {
                                scope.launch {
                                    pagerState.animateScrollToPrevPage()
                                }
                            } else if (offset.x >= size.width / 5 * 4) {
                                scope.launch {
                                    pagerState.animateScrollToNextPage()
                                }
                            } else {
                                val url = carousel.items[pagerState.currentPage].image
                                if (url != null) {
                                    onImageClick(url)
                                }
                            }
                        },
                        onLongPress = {
                            val url = carousel.items[pagerState.currentPage].image
                            if (url != null) {
                                onImageLongClick(url)
                            }
                            feedback.performLongPress()
                        },
                    )
                },
            state = pagerState,
        ) {
            val item = carousel.items[it]
            val image = item.image
            val video = item.video

            if (!video.isNullOrEmpty()) {
                VideoView(
                    state = rememberVideoPlaybackState(url = video),
                    modifier = Modifier.fillMaxSize(),
                )
            } else if (!image.isNullOrEmpty()) {
                AsyncImage(
                    request = ImageRequest.Downloadable(image),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (it % 2 == 0) evenColor else oddColor),
                    showProgressbar = true,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.End)
                .background(borderColor)
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_baseline_view_carousel_24),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )

            Indicator(
                pageCount = pageCount,
                pagerState = pagerState,
                modifier = Modifier.weight(weight = 1f, fill = false),
            )

            Text(
                text = "${pagerState.currentPage + 1} / $pageCount",
                fontSize = 14.sp,
            )
        }
    }
}

@OptIn(
    ExperimentalFoundationApi::class,
    com.google.accompanist.pager.ExperimentalPagerApi::class,
)
@Composable
private fun Indicator(
    pageCount: Int,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    dotSize: Dp = 8.dp,
    dotSpacing: Dp = 2.dp,
    maxDots: Int = 10,
) {
    val hiddenDotCount = pagerState.currentPage / 10 * maxDots
    val dotCount = min(pageCount - hiddenDotCount, maxDots)
    if (dotCount == 0) {
        return
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pagerState.currentPage >= maxDots) {
            Text(
                text = "+${hiddenDotCount}",
                fontSize = 14.sp,
                color = MaterialTheme.colors.secondaryText,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        val selectedTabIndex = pagerState.currentPage % maxDots
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = modifier.width((dotSize + dotSpacing * 2) * dotCount),
            backgroundColor = Color.Transparent,
            indicator = { tabPositions ->
                val indicatorColor = MaterialTheme.colors.primary
                Spacer(
                    modifier = Modifier
                        .pagerTabIndicatorOffset(
                            pagerState = pagerState,
                            tabPositions = tabPositions,
                            pageIndexMapping = { index -> index % maxDots }
                        )
                        .offset { IntOffset(0, -dotSize.roundToPx() / 2) }
                        .drawBehind {
                            drawCircle(
                                color = indicatorColor,
                                radius = dotSize.toPx() / 2,
                            )
                        }
                )
            },
            divider = {},
        ) {
            repeat(dotCount) {
                Spacer(
                    modifier = Modifier
                        .padding(horizontal = dotSpacing)
                        .size(dotSize)
                        .background(
                            color = MaterialTheme.colors.onBackground.copy(alpha = 0.12f),
                            shape = CircleShape,
                        )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
private suspend fun PagerState.animateScrollToPrevPage() {
    if (canScrollForward) {
        animateScrollToPage(currentPage - 1)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private suspend fun PagerState.animateScrollToNextPage() {
    if (canScrollForward) {
        animateScrollToPage(currentPage + 1)
    }
}