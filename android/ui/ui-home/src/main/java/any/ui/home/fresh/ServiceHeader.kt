package any.ui.home.fresh

import any.base.R as BaseR
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import any.base.compose.ImmutableHolder
import any.base.image.ImageRequest
import any.ui.common.image.AsyncImage
import any.ui.common.theme.sizes
import any.ui.common.widget.DefaultServiceHeader
import any.ui.home.HomeScreenDefaults

@Composable
internal fun ServiceHeader(
    currentServiceName: String?,
    modifier: Modifier = Modifier,
    scrollProvider: () -> Int,
    headerPicUrl: String? = null,
    themeColor: Color = MaterialTheme.colors.primary,
    bottomMaskColor: Color = MaterialTheme.colors.background,
    defaultHeaderIcons: ImmutableHolder<List<ImageRequest>> = ImmutableHolder(emptyList()),
    footerContent: @Composable () -> Unit = {},
) {
    var heightPx by remember { mutableStateOf(0) }

    fun calcScrollProgress(): Float {
        return if (heightPx > 0) {
            (-scrollProvider().toFloat() / heightPx).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .onSizeChanged { heightPx = it.height }
    ) {
        var contentHeight by remember { mutableStateOf(0) }

        Column {
            val maxImageHeight = with(LocalDensity.current) {
                (LocalView.current.measuredHeight / 2).toDp()
            }
            Box(
                modifier = Modifier
                    .heightIn(max = maxImageHeight)
                    .fillMaxWidth()
                    .aspectRatio(MaterialTheme.sizes.headerPicAspectRatio)
                    .graphicsLayer {
                        val scrollProgress = calcScrollProgress()
                        transformOrigin = TransformOrigin.Center

                        val maxTranY = heightPx * 0.5f
                        translationY = scrollProgress * maxTranY

                        alpha = 1f - scrollProgress * 0.8f
                    },
            ) {
                if (!headerPicUrl.isNullOrEmpty()) {
                    AsyncImage(
                        request = ImageRequest.Url(headerPicUrl),
                        contentDescription = stringResource(BaseR.string.header_image),
                        modifier = Modifier
                            .background(MaterialTheme.colors.onBackground.copy(alpha = 0.3f)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    DefaultServiceHeader(
                        currentServiceName = currentServiceName,
                        icons = defaultHeaderIcons,
                        blockColor = themeColor,
                    )
                }
            }

            val density = LocalDensity.current
            val height = with(density) { contentHeight.toDp() / 2 }
            Spacer(modifier = Modifier.height(height))
        }

        val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .drawWithCache {
                    val path = Path()
                    val h1 = contentHeight * 1.5f
                    val h2 = contentHeight * 1.35f
                    val h3 = contentHeight * 1.2f
                    val extraHeights = arrayOf(
                        56.dp.toPx(),
                        32.dp.toPx(),
                        16.dp.toPx(),
                    )
                    onDrawBehind {
                        val scrollProgress = calcScrollProgress()
                        var extraHeight = extraHeights[0] * scrollProgress
                        drawBottomAlignTrapezoid(
                            path = path,
                            startEdgeLength = h1 + extraHeight,
                            endEdgeLength = h1 * 0.5f + extraHeight,
                            color = bottomMaskColor.copy(alpha = 0.2f),
                            isLtr = isLtr,
                        )
                        extraHeight = extraHeights[1] * scrollProgress
                        drawBottomAlignTrapezoid(
                            path = path,
                            startEdgeLength = h2 + extraHeight,
                            endEdgeLength = h2 * 0.5f + extraHeight,
                            color = bottomMaskColor.copy(alpha = 0.6f),
                            isLtr = isLtr,
                        )
                        extraHeight = extraHeights[2] * scrollProgress
                        drawBottomAlignTrapezoid(
                            path = path,
                            startEdgeLength = h3 + extraHeight,
                            endEdgeLength = h3 * 0.5f + extraHeight,
                            color = bottomMaskColor,
                            isLtr = isLtr,
                        )
                    }
                }
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = HomeScreenDefaults.HorizontalPadding)
                .onSizeChanged { contentHeight = it.height }
        ) {
            footerContent()
        }
    }
}

private fun DrawScope.drawBottomAlignTrapezoid(
    path: Path,
    startEdgeLength: Float,
    endEdgeLength: Float,
    color: Color,
    isLtr: Boolean,
) {
    val bottomStart = Offset(0f, size.height)
    val bottomEnd = Offset(size.width, size.height)
    fillBottomAlignTrapezoid(
        path = path,
        bottomStartPoint = bottomStart,
        bottomEndPoint = bottomEnd,
        startEdgeLength = if (isLtr) startEdgeLength else endEdgeLength,
        endEdgeLength = if (isLtr) endEdgeLength else startEdgeLength,
    )
    drawPath(path, color)
}

/**
 * bottom align trapezoid:
 *
 * | -
 * |        -
 * |               -
 * |                   |
 * |                   |
 * |                   |
 * |___________________|
 */
private fun fillBottomAlignTrapezoid(
    path: Path,
    bottomStartPoint: Offset,
    bottomEndPoint: Offset,
    startEdgeLength: Float,
    endEdgeLength: Float,
) {
    path.reset()
    // Top start point
    path.moveTo(bottomStartPoint.x, bottomStartPoint.y - startEdgeLength)
    // Start edge
    path.lineTo(bottomStartPoint.x, bottomStartPoint.y)
    // Bottom edge
    path.lineTo(bottomEndPoint.x, bottomEndPoint.y)
    // End edge
    path.lineTo(bottomEndPoint.x, bottomEndPoint.y - endEdgeLength)
    // Close
    path.close()
}