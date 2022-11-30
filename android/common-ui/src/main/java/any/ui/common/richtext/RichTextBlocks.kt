package any.ui.common.richtext

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import any.base.image.ImageRequest
import any.ui.common.LocalFontScale
import any.ui.common.image.AsyncImage

object RichTextBlocks {
    private val HeadingSizes = floatArrayOf(1.5f, 1.4f, 1.3f, 1.2f, 1.1f, 1f)

    private val HeadingSpacings = arrayOf(16.dp, 14.dp, 12.dp, 10.dp, 8.dp, 6.dp)

    val DefElementContentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)

    @Composable
    fun Heading(
        text: AnnotatedString,
        level: Int,
        onTextLayout: (TextLayoutResult) -> Unit,
        modifier: Modifier = Modifier,
        selectionEnabled: Boolean = true,
        contentPadding: PaddingValues = DefElementContentPadding,
        showHorizontalRule: Boolean = false,
        color: Color = MaterialTheme.colors.onBackground,
        maxLines: Int = Int.MAX_VALUE,
        textOverflow: TextOverflow = TextOverflow.Clip,
    ) {
        require(level in 1..6) { "Heading level must in range [1, 6]" }
        Column(
            modifier = modifier
                .padding(contentPadding)
                .padding(
                    top = HeadingSpacings[level - 1],
                    bottom = HeadingSpacings[level - 1],
                )
                .fillMaxWidth(),
        ) {
            SelectionWrapper(enabled = selectionEnabled) {
                val fontScale = HeadingSizes[level - 1] * LocalFontScale.current
                androidx.compose.material.Text(
                    text = text,
                    color = color,
                    fontSize = LocalTextStyle.current.fontSize * fontScale,
                    fontWeight = FontWeight.Bold,
                    style = LocalTextStyle.current.copy(
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None,
                        ),
                    ),
                    onTextLayout = onTextLayout,
                    maxLines = maxLines,
                    overflow = textOverflow,
                )
            }

            if (showHorizontalRule) {
                Divider(modifier = Modifier.padding(top = 8.dp))
            }
        }
    }

    @Composable
    fun Text(
        text: AnnotatedString,
        onTextLayout: (TextLayoutResult) -> Unit,
        modifier: Modifier = Modifier,
        selectionEnabled: Boolean = true,
        contentPadding: PaddingValues = DefElementContentPadding,
        color: Color = MaterialTheme.colors.onBackground,
        maxLines: Int = Int.MAX_VALUE,
        lineHeight: TextUnit = LocalTextStyle.current.lineHeight,
        textOverflow: TextOverflow = TextOverflow.Clip,
    ) {
        SelectionWrapper(enabled = selectionEnabled) {
            androidx.compose.material.Text(
                text = text,
                modifier = modifier.padding(contentPadding),
                color = color,
                fontSize = LocalTextStyle.current.fontSize * LocalFontScale.current,
                style = LocalTextStyle.current.copy(
                    color = MaterialTheme.colors.onBackground,
                    lineHeight = lineHeight,
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Center,
                        trim = LineHeightStyle.Trim.None,
                    ),
                ),
                onTextLayout = onTextLayout,
                maxLines = maxLines,
                overflow = textOverflow,
            )
        }
    }

    @Composable
    fun Image(
        url: String,
        contentDescription: String?,
        modifier: Modifier = Modifier,
        contentPadding: PaddingValues = DefElementContentPadding,
    ) {
        AsyncImage(
            request = ImageRequest.Url(url),
            contentDescription = contentDescription,
            modifier = modifier
                .fillMaxWidth()
                .padding(contentPadding),
        )
    }

    @Composable
    fun OrderedListItem(
        order: Int,
        modifier: Modifier = Modifier,
        indentWidth: Dp = 16.dp,
        gapWidth: Dp = 8.dp,
        selectionEnabled: Boolean = true,
        contentPadding: PaddingValues = DefElementContentPadding,
        orderTextStyle: TextStyle = LocalTextStyle.current,
        content: @Composable () -> Unit,
    ) {
        SelectionWrapper(enabled = selectionEnabled) {
            Row(
                modifier = modifier
                    .padding(contentPadding)
                    .padding(start = indentWidth),
            ) {
                androidx.compose.material.Text(
                    text = "$order.",
                    style = orderTextStyle,
                )
                Spacer(modifier = Modifier.width(gapWidth))
                content()
            }
        }
    }

    @Composable
    fun UnorderedListItem(
        modifier: Modifier = Modifier,
        indentWidth: Dp = 16.dp,
        dotRadius: Dp = 3.dp,
        gapWidth: Dp = 8.dp,
        dotColor: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.7f),
        selectionEnabled: Boolean = true,
        contentPadding: PaddingValues = DefElementContentPadding,
        content: @Composable () -> Unit,
    ) {
        val lineHeight = LocalTextStyle.current.lineHeight
        val fontSize = LocalTextStyle.current.fontSize
        SelectionWrapper(enabled = selectionEnabled) {
            Box(
                modifier = modifier
                    .drawWithCache {
                        val dotR = dotRadius.toPx()
                        val dotCenterX = indentWidth.toPx() + dotR
                        val dotCenterY = if (lineHeight.isSp) {
                            lineHeight.toPx() / 2f
                        } else if (lineHeight.isEm && fontSize.isSp) {
                            (lineHeight.value * fontSize.toPx()) / 2f
                        } else if (fontSize.isSp) {
                            fontSize.toPx() * 1.4f / 2f
                        } else {
                            (fontSize.value * 16.sp.toPx()) / 2
                        } + contentPadding
                            .calculateTopPadding()
                            .toPx()
                        onDrawBehind {
                            drawCircle(
                                color = dotColor,
                                radius = dotR,
                                center = Offset(dotCenterX, dotCenterY),
                            )
                        }
                    }
                    .padding(contentPadding)
                    .padding(start = indentWidth + dotRadius * 2 + gapWidth),
                content = { content() },
            )
        }
    }

    @Composable
    fun BlockQuote(
        modifier: Modifier = Modifier,
        stripColor: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.06f),
        stripWidth: Dp = 6.dp,
        gapWidth: Dp = 8.dp,
        selectionEnabled: Boolean = true,
        contentPadding: PaddingValues = DefElementContentPadding,
        content: @Composable () -> Unit,
    ) {
        SelectionWrapper(enabled = selectionEnabled) {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(contentPadding)
                    .drawBehind {
                        drawRect(
                            color = stripColor,
                            size = size.copy(width = stripWidth.toPx()),
                        )
                    }
                    .padding(start = stripWidth + gapWidth),
                content = { content() },
            )
        }
    }

    @Composable
    fun CodeBlock(
        text: String,
        language: String?,
        modifier: Modifier = Modifier,
        selectionEnabled: Boolean = true,
        contentPadding: PaddingValues = DefElementContentPadding,
        backgroundColor: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.06f),
        color: Color = MaterialTheme.colors.onBackground,
        maxLines: Int = Int.MAX_VALUE,
        lineHeight: TextUnit = LocalTextStyle.current.lineHeight,
        textOverflow: TextOverflow = TextOverflow.Clip,
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(contentPadding)
                .clip(MaterialTheme.shapes.small)
                .background(backgroundColor)
                .horizontalScroll(state = rememberScrollState())
        ) {
            SelectionWrapper(enabled = selectionEnabled) {
                androidx.compose.material.Text(
                    text = text,
                    modifier = Modifier.padding(12.dp),
                    color = color,
                    fontSize = 14.sp * LocalFontScale.current,
                    fontFamily = FontFamily.Monospace,
                    style = LocalTextStyle.current.copy(
                        lineHeight = lineHeight,
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.None,
                        ),
                    ),
                    maxLines = maxLines,
                    overflow = textOverflow,
                )
            }
        }
    }

    @Composable
    fun HorizontalRule(
        modifier: Modifier = Modifier,
        thickness: Dp = 3.dp,
        contentPadding: PaddingValues = DefElementContentPadding,
        color: Color = MaterialTheme.colors.onBackground.copy(alpha = 0.06f),
    ) {
        Spacer(
            modifier = modifier
                .fillMaxWidth()
                .padding(contentPadding)
                .height(thickness)
                .background(color),
        )
    }

    @Composable
    private fun SelectionWrapper(
        enabled: Boolean,
        content: @Composable () -> Unit,
    ) {
        if (enabled) {
            SelectionContainer(content = content)
        } else {
            content()
        }
    }
}